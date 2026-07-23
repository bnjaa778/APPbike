using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Collections;
using System.Net;
using System.Security.Cryptography;
using System.Security.Principal;
using System.Text;
using System.Threading;
using System.Web.Script.Serialization;
using System.Windows.Forms;

// Fuente del puente EasyMD, centralizada junto al historial operativo de Codex.
namespace EasyMD
{
    internal static class Program
    {
        [STAThread]
        private static void Main(string[] args)
        {
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

            if (args.Length > 0 && args[0].Equals("--watch", StringComparison.OrdinalIgnoreCase))
            {
                Watcher.Run();
                return;
            }

            if (args.Length > 1 && args[0].Equals("--export-endpoint", StringComparison.OrdinalIgnoreCase))
            {
                EasyConfig cfg = Store.Load();
                if (string.IsNullOrWhiteSpace(cfg.Token))
                {
                    Console.Error.WriteLine("EasyMD no tiene token configurado.");
                    Environment.Exit(1);
                }
                File.WriteAllText(args[1], EndpointFactory.CreatePhp(cfg.Token), Encoding.UTF8);
                return;
            }

            if (args.Length > 0 && args[0].Equals("--self-test-codex", StringComparison.OrdinalIgnoreCase))
            {
                Environment.Exit(SelfTest.RunCodex());
            }

            if (args.Length > 0 && args[0].Equals("--self-test-roundtrip", StringComparison.OrdinalIgnoreCase))
            {
                Environment.Exit(SelfTest.RunRoundTrip());
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }

    internal static class SelfTest
    {
        public static int RunCodex()
        {
            EasyConfig cfg = Store.Load();
            Dictionary<string, object> task = new Dictionary<string, object>();
            task["id"] = "easymd-selftest-" + DateTime.UtcNow.ToString("yyyyMMdd-HHmmss");
            task["title"] = "Prueba local capturada de Codex";
            task["source"] = cfg.Role;
            task["target"] = cfg.Role;
            task["project"] = cfg.Project;
            task["created_at"] = DateTime.UtcNow.ToString("o");
            task["markdown"] = "# Prueba local EasyMD\n\nResponde claramente que Codex recibio esta prueba local. No modifiques archivos.";
            string path = Watcher.SaveTask(cfg, task);
            CodexRunResult result = CodexRunner.RunAndWait(cfg, task, path);
            Console.WriteLine(result.Success ? result.FinalMessage : result.ErrorMessage);
            Console.WriteLine("stdout=" + result.StdoutPath);
            Console.WriteLine("stderr=" + result.StderrPath);
            return result.Success ? 0 : 1;
        }

        public static int RunRoundTrip()
        {
            EasyConfig cfg = Store.Load();
            if (string.IsNullOrWhiteSpace(cfg.Token) || string.IsNullOrWhiteSpace(cfg.Role))
            {
                Console.Error.WriteLine("EasyMD requiere rol y token para la prueba de ida y vuelta.");
                return 1;
            }

            string stamp = DateTime.UtcNow.ToString("yyyyMMdd-HHmmss");
            string testProject = cfg.Project + "-selftest-" + stamp;
            string testDir = Path.Combine(Path.GetTempPath(), "EasyMD-roundtrip-" + stamp);
            Directory.CreateDirectory(testDir);
            cfg.Project = testProject;
            cfg.OutDir = testDir;
            cfg.WorkspaceDir = testDir;
            cfg.CodexArguments = "exec --ephemeral --sandbox read-only --skip-git-repo-check \"Lee la tarea EasyMD en {file} y confirma en tu respuesta final que completaste la prueba de ida y vuelta. No modifiques archivos.\"";

            Dictionary<string, object> pushed = EasyApi.Call(cfg, new Dictionary<string, object>
            {
                { "action", "push" },
                { "source", cfg.Role },
                { "target", cfg.Role },
                { "project", testProject },
                { "title", "Prueba ida y vuelta " + stamp },
                { "markdown", "# Prueba ida y vuelta EasyMD\n\nConfirma que recibiste esta tarea local de diagnostico." }
            });
            Dictionary<string, object> original = pushed.ContainsKey("task") ? pushed["task"] as Dictionary<string, object> : null;
            if (original == null)
            {
                Console.Error.WriteLine("El endpoint no devolvio la tarea de prueba.");
                return 1;
            }

            Dictionary<string, object> claimedResponse = EasyApi.Call(cfg, new Dictionary<string, object>
            {
                { "action", "next" },
                { "target", cfg.Role },
                { "project", testProject },
                { "claimer", cfg.Role + "-selftest" }
            });
            Dictionary<string, object> claimed = claimedResponse.ContainsKey("task") ? claimedResponse["task"] as Dictionary<string, object> : null;
            if (claimed == null)
            {
                Console.Error.WriteLine("No se pudo reclamar la tarea local de prueba.");
                return 1;
            }

            string path = Watcher.SaveTask(cfg, claimed);
            TaskProcessor.ProcessRemoteTask(cfg, claimed, path, Console.WriteLine);

            Dictionary<string, object> listed = EasyApi.Call(cfg, new Dictionary<string, object>
            {
                { "action", "list" },
                { "project", testProject },
                { "source", cfg.Role },
                { "target", cfg.Role }
            });
            IEnumerable items = listed.ContainsKey("tasks") ? listed["tasks"] as IEnumerable : null;
            Dictionary<string, object> reply = null;
            bool originalDone = false;
            if (items != null)
            {
                foreach (object item in items)
                {
                    Dictionary<string, object> task = item as Dictionary<string, object>;
                    if (task == null) continue;
                    string id = Value(task, "id");
                    if (id == Value(original, "id")) originalDone = Value(task, "status") == "done";
                    else if (Value(task, "title").StartsWith("Respuesta EasyMD a ", StringComparison.Ordinal)) reply = task;
                }
            }

            bool valid = originalDone && reply != null && Value(reply, "markdown").Contains(Value(original, "id"));
            if (reply != null)
            {
                valid = valid && TaskProcessor.IsResponse(reply);
                TaskProcessor.CompleteReceivedResponse(cfg, reply, Console.WriteLine);
            }
            Console.WriteLine(valid ? "EASYMD_ROUNDTRIP_OK" : "EASYMD_ROUNDTRIP_FAILED");
            Console.WriteLine("project=" + testProject);
            Console.WriteLine("original=" + Value(original, "id"));
            Console.WriteLine("reply=" + (reply == null ? "" : Value(reply, "id")));
            return valid ? 0 : 1;
        }

        private static string Value(Dictionary<string, object> dict, string key)
        {
            return dict.ContainsKey(key) && dict[key] != null ? Convert.ToString(dict[key]) : "";
        }
    }

    internal sealed class EasyConfig
    {
        public const string LegacyCodexArguments = "exec \"Lee y ejecuta la tarea EasyMD guardada en: {file}\"";
        public const string DefaultCodexArguments = "exec --sandbox workspace-write \"Lee y ejecuta la tarea EasyMD guardada en: {file}\"";

        public string Role = "";
        public string Token = "";
        public string BaseUrl = "https://api.zizzio.cl/easymd";
        public string Project = "APPbike";
        public string OutDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "EasyMD Inbox");
        public int IntervalSeconds = 60;
        public bool AutoRunCodex = false;
        public string CodexCommand = Store.FindCodexCommand();
        public string CodexArguments = DefaultCodexArguments;
        public string WorkspaceDir = Environment.CurrentDirectory;
    }

    internal static class Store
    {
        public static readonly string Dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "EasyMD");
        public static readonly string ConfigPath = Path.Combine(Dir, "config.json");
        public static readonly string PidPath = Path.Combine(Dir, "watch.pid");
        public static readonly string LogPath = Path.Combine(Dir, "watch.log");
        public static readonly string ErrorLogPath = Path.Combine(Dir, "watch.err.log");
        private static readonly JavaScriptSerializer Json = new JavaScriptSerializer();

        public static EasyConfig Load()
        {
            Directory.CreateDirectory(Dir);
            if (!File.Exists(ConfigPath))
            {
                return new EasyConfig();
            }

            try
            {
                Dictionary<string, object> raw = Json.Deserialize<Dictionary<string, object>>(File.ReadAllText(ConfigPath, Encoding.UTF8));
                EasyConfig cfg = new EasyConfig();
                if (raw.ContainsKey("role")) cfg.Role = Convert.ToString(raw["role"]);
                if (raw.ContainsKey("token")) cfg.Token = Convert.ToString(raw["token"]);
                if (raw.ContainsKey("baseUrl")) cfg.BaseUrl = Convert.ToString(raw["baseUrl"]);
                if (raw.ContainsKey("project")) cfg.Project = Convert.ToString(raw["project"]);
                if (raw.ContainsKey("outDir")) cfg.OutDir = Convert.ToString(raw["outDir"]);
                if (raw.ContainsKey("intervalSeconds")) cfg.IntervalSeconds = Convert.ToInt32(raw["intervalSeconds"]);
                if (raw.ContainsKey("autoRunCodex")) cfg.AutoRunCodex = Convert.ToBoolean(raw["autoRunCodex"]);
                if (raw.ContainsKey("codexCommand")) cfg.CodexCommand = Convert.ToString(raw["codexCommand"]);
                if (raw.ContainsKey("codexArguments")) cfg.CodexArguments = Convert.ToString(raw["codexArguments"]);
                if (cfg.CodexArguments == EasyConfig.LegacyCodexArguments) cfg.CodexArguments = EasyConfig.DefaultCodexArguments;
                if (raw.ContainsKey("workspaceDir")) cfg.WorkspaceDir = Convert.ToString(raw["workspaceDir"]);
                return cfg;
            }
            catch
            {
                return new EasyConfig();
            }
        }

        public static void Save(EasyConfig cfg)
        {
            Directory.CreateDirectory(Dir);
            Dictionary<string, object> raw = new Dictionary<string, object>();
            raw["role"] = cfg.Role ?? "";
            raw["token"] = cfg.Token ?? "";
            raw["baseUrl"] = string.IsNullOrWhiteSpace(cfg.BaseUrl) ? "https://api.zizzio.cl/easymd" : cfg.BaseUrl.TrimEnd('/');
            raw["project"] = string.IsNullOrWhiteSpace(cfg.Project) ? "APPbike" : cfg.Project;
            raw["outDir"] = string.IsNullOrWhiteSpace(cfg.OutDir) ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "EasyMD Inbox") : cfg.OutDir;
            raw["intervalSeconds"] = cfg.IntervalSeconds <= 0 ? 60 : cfg.IntervalSeconds;
            raw["autoRunCodex"] = cfg.AutoRunCodex;
            raw["codexCommand"] = string.IsNullOrWhiteSpace(cfg.CodexCommand) ? FindCodexCommand() : cfg.CodexCommand;
            raw["codexArguments"] = string.IsNullOrWhiteSpace(cfg.CodexArguments) ? EasyConfig.DefaultCodexArguments : cfg.CodexArguments;
            raw["workspaceDir"] = string.IsNullOrWhiteSpace(cfg.WorkspaceDir) ? Environment.CurrentDirectory : cfg.WorkspaceDir;
            File.WriteAllText(ConfigPath, Json.Serialize(raw), Encoding.UTF8);
        }

        public static string FindCodexCommand()
        {
            try
            {
                string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                string codexBin = Path.Combine(local, "OpenAI", "Codex", "bin");
                if (Directory.Exists(codexBin))
                {
                    string[] matches = Directory.GetFiles(codexBin, "codex.exe", SearchOption.AllDirectories);
                    if (matches.Length > 0)
                    {
                        Array.Sort(matches);
                        return matches[matches.Length - 1];
                    }
                }
            }
            catch { }
            try
            {
                string found = FindOnPath("codex.exe");
                if (!string.IsNullOrWhiteSpace(found)) return found;
            }
            catch { }
            try
            {
                string programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
                string windowsApps = Path.Combine(programFiles, "WindowsApps");
                if (Directory.Exists(windowsApps))
                {
                    string[] matches = Directory.GetFiles(windowsApps, "codex.exe", SearchOption.AllDirectories);
                    if (matches.Length > 0)
                    {
                        Array.Sort(matches);
                        return matches[matches.Length - 1];
                    }
                }
            }
            catch { }
            return "codex";
        }

        public static string FindOnPath(string fileName)
        {
            string path = Environment.GetEnvironmentVariable("PATH") ?? "";
            foreach (string dir in path.Split(Path.PathSeparator))
            {
                if (string.IsNullOrWhiteSpace(dir)) continue;
                try
                {
                    string candidate = Path.Combine(dir.Trim('"'), fileName);
                    if (File.Exists(candidate)) return candidate;
                }
                catch { }
            }
            return "";
        }
    }

    internal static class EasyApi
    {
        private static readonly JavaScriptSerializer Json = new JavaScriptSerializer();

        public static Dictionary<string, object> Call(EasyConfig cfg, Dictionary<string, object> payload)
        {
            if (string.IsNullOrWhiteSpace(cfg.Token))
            {
                throw new InvalidOperationException("Falta token. Configura EasyMD primero.");
            }

            string url = (string.IsNullOrWhiteSpace(cfg.BaseUrl) ? "https://api.zizzio.cl/easymd" : cfg.BaseUrl.TrimEnd('/')) + "/index.php";
            byte[] bytes = Encoding.UTF8.GetBytes(Json.Serialize(payload));

            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "POST";
            request.ContentType = "application/json; charset=utf-8";
            request.Headers["X-EasyMD-Token"] = cfg.Token;
            request.Timeout = 15000;
            request.ReadWriteTimeout = 15000;
            request.ContentLength = bytes.Length;

            using (Stream stream = request.GetRequestStream())
            {
                stream.Write(bytes, 0, bytes.Length);
            }

            try
            {
                using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                using (StreamReader reader = new StreamReader(response.GetResponseStream(), Encoding.UTF8))
                {
                    return Json.Deserialize<Dictionary<string, object>>(reader.ReadToEnd());
                }
            }
            catch (WebException ex)
            {
                string status = "";
                if (ex.Response != null)
                {
                    HttpWebResponse errorResponse = ex.Response as HttpWebResponse;
                    if (errorResponse != null)
                    {
                        status = ((int)errorResponse.StatusCode) + " " + errorResponse.StatusDescription;
                    }

                    using (StreamReader reader = new StreamReader(ex.Response.GetResponseStream(), Encoding.UTF8))
                    {
                        string body = reader.ReadToEnd();
                        if (string.IsNullOrWhiteSpace(body))
                        {
                            body = "El servidor respondio " + status + " sin entregar detalle. Revisa el index.php publicado o la version de PHP del hosting.";
                        }
                        throw new InvalidOperationException(body, ex);
                    }
                }
                throw;
            }
        }
    }

    internal static class TokenFactory
    {
        public static string Create(string[] pairs, string phraseA, string phraseB)
        {
            StringBuilder seed = new StringBuilder();
            seed.Append(Environment.MachineName).Append("|");
            seed.Append(Environment.UserName).Append("|");
            seed.Append(DateTime.UtcNow.Ticks).Append("|");
            seed.Append(Guid.NewGuid()).Append("|");
            for (int i = 0; i < pairs.Length; i++)
            {
                seed.Append(pairs[i]).Append("|");
            }
            seed.Append(phraseA).Append("|").Append(phraseB).Append("|");

            byte[] random = new byte[64];
            using (RandomNumberGenerator rng = RandomNumberGenerator.Create())
            {
                rng.GetBytes(random);
            }
            seed.Append(Convert.ToBase64String(random));

            using (SHA512 sha = SHA512.Create())
            {
                byte[] hash = sha.ComputeHash(Encoding.UTF8.GetBytes(seed.ToString()));
                return Convert.ToBase64String(hash).TrimEnd('=').Replace('+', '-').Replace('/', '_');
            }
        }
    }

    internal static class EndpointFactory
    {
        public static string CreatePhp(string token)
        {
            string safeToken = token.Replace("\\", "\\\\").Replace("'", "\\'");
            return CreatePhpCompat(safeToken);

            return @"<?php
declare(strict_types=1);
const EASYMD_TOKEN = '" + safeToken + @"';
const EASYMD_VERSION = '1.2.0';
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
function reply(int $code, array $data): void { http_response_code($code); echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT); exit; }
function body(): array { $raw = file_get_contents('php://input'); if ($raw === false || trim($raw) === '') return []; $json = json_decode($raw, true); if (is_array($json)) return $json; parse_str($raw, $form); return is_array($form) ? $form : []; }
function req_token(array $body): string { $h = function_exists('getallheaders') ? getallheaders() : []; foreach ($h as $n => $v) { if (strcasecmp((string)$n, 'X-EasyMD-Token') === 0) return trim((string)$v); if (strcasecmp((string)$n, 'Authorization') === 0) { $a = trim((string)$v); if (stripos($a, 'Bearer ') === 0) return trim(substr($a, 7)); } } if (isset($body['token'])) return trim((string)$body['token']); return isset($_GET['token']) ? trim((string)$_GET['token']) : ''; }
function auth(array $body): void { if (EASYMD_TOKEN === '' || !hash_equals(EASYMD_TOKEN, req_token($body))) reply(401, ['ok'=>false, 'error'=>'unauthorized']); }
function data_dir(): string { $d = __DIR__ . DIRECTORY_SEPARATOR . '_data'; if (!is_dir($d) && !mkdir($d, 0750, true) && !is_dir($d)) reply(500, ['ok'=>false, 'error'=>'data_dir_unavailable']); $ht = $d . DIRECTORY_SEPARATOR . '.htaccess'; if (!file_exists($ht)) @file_put_contents($ht, ""Require all denied\nDeny from all\n""); return $d; }
function tasks_file(): string { return data_dir() . DIRECTORY_SEPARATOR . 'tasks.json'; }
function lock_file(): string { return data_dir() . DIRECTORY_SEPARATOR . 'tasks.lock'; }
function now_iso(): string { return gmdate('c'); }
function text($v, string $f = ''): string { if (!is_scalar($v)) return $f; $t = trim((string)$v); return $t === '' ? $f : $t; }
function status_value($v, string $f = 'queued'): string { $s = text($v, $f); return in_array($s, ['queued','claimed','done','cancelled'], true) ? $s : $f; }
function load_tasks(): array { $f = tasks_file(); if (!file_exists($f)) return []; $raw = file_get_contents($f); $json = $raw === false ? null : json_decode($raw, true); return is_array($json) ? array_values($json) : []; }
function save_tasks(array $tasks): void { $f = tasks_file(); $tmp = $f . '.tmp'; $json = json_encode(array_values($tasks), JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT); if ($json === false || file_put_contents($tmp, $json, LOCK_EX) === false || !rename($tmp, $f)) { @unlink($tmp); reply(500, ['ok'=>false, 'error'=>'write_failed']); } }
function locked(callable $fn): array { $h = fopen(lock_file(), 'c+'); if ($h === false || !flock($h, LOCK_EX)) reply(500, ['ok'=>false, 'error'=>'lock_failed']); try { $tasks = load_tasks(); $result = $fn($tasks); if (isset($result['__tasks'])) { save_tasks($result['__tasks']); unset($result['__tasks']); } flock($h, LOCK_UN); fclose($h); return $result; } catch (Throwable $e) { flock($h, LOCK_UN); fclose($h); reply(500, ['ok'=>false, 'error'=>'internal_error', 'message'=>$e->getMessage()]); } }
function match_task(array $task, array $filter): bool { foreach (['project','target','source','status'] as $k) { if (!isset($filter[$k]) || $filter[$k] === '') continue; if (($task[$k] ?? '') !== $filter[$k]) return false; } return true; }
$body = body(); $action = text($body['action'] ?? $_GET['action'] ?? 'status', 'status'); auth($body);
if ($action === 'status' || $action === 'ping') reply(200, locked(function(array $tasks): array { $c = ['queued'=>0,'claimed'=>0,'done'=>0,'cancelled'=>0]; foreach ($tasks as $t) $c[status_value($t['status'] ?? 'queued')]++; return ['ok'=>true,'version'=>EASYMD_VERSION,'counts'=>$c]; }));
if ($action === 'push') { $md = text($body['markdown'] ?? $body['text'] ?? ''); if ($md === '') reply(422, ['ok'=>false,'error'=>'missing_markdown']); $now = now_iso(); $task = ['id'=>'easymd-' . gmdate('Ymd-His') . '-' . bin2hex(random_bytes(4)), 'created_at'=>$now, 'updated_at'=>$now, 'source'=>text($body['source'] ?? 'unknown'), 'target'=>text($body['target'] ?? 'desktop'), 'project'=>text($body['project'] ?? 'APPbike'), 'title'=>text($body['title'] ?? 'Untitled handoff'), 'priority'=>text($body['priority'] ?? 'normal'), 'status'=>'queued', 'claimed_at'=>null, 'completed_at'=>null, 'claim_count'=>0, 'markdown'=>$md, 'history'=>[['at'=>$now,'event'=>'created','by'=>text($body['source'] ?? 'unknown')]]]; reply(201, locked(function(array $tasks) use ($task): array { $tasks[] = $task; return ['ok'=>true,'task'=>$task,'__tasks'=>$tasks]; })); }
if ($action === 'list') { $filter = ['project'=>text($body['project'] ?? ''), 'target'=>text($body['target'] ?? ''), 'source'=>text($body['source'] ?? ''), 'status'=>text($body['status'] ?? '')]; reply(200, locked(function(array $tasks) use ($filter): array { $items = array_values(array_filter($tasks, function($task) use ($filter): bool { return match_task($task, $filter); })); usort($items, function($a,$b): int { return strcmp((string)($b['created_at'] ?? ''), (string)($a['created_at'] ?? '')); }); return ['ok'=>true,'tasks'=>array_slice($items,0,50)]; })); }
if ($action === 'next') { $target = text($body['target'] ?? 'desktop'); $project = text($body['project'] ?? 'APPbike'); $claimer = text($body['claimer'] ?? $target); reply(200, locked(function(array $tasks) use ($target,$project,$claimer): array { foreach ($tasks as $i => $t) { if (($t['target'] ?? '') === $target && ($t['project'] ?? '') === $project && ($t['status'] ?? '') === 'queued') { $now = now_iso(); $tasks[$i]['status'] = 'claimed'; $tasks[$i]['claimed_at'] = $now; $tasks[$i]['updated_at'] = $now; $tasks[$i]['claim_count'] = (int)($tasks[$i]['claim_count'] ?? 0) + 1; $tasks[$i]['history'][] = ['at'=>$now,'event'=>'claimed','by'=>$claimer]; return ['ok'=>true,'task'=>$tasks[$i],'__tasks'=>$tasks]; } } return ['ok'=>true,'task'=>null,'message'=>'No queued task.']; })); }
if ($action === 'done' || $action === 'update') { $id = text($body['id'] ?? ''); $status = status_value($body['status'] ?? ($action === 'done' ? 'done' : 'queued')); $by = text($body['by'] ?? 'unknown'); $note = text($body['note'] ?? ''); if ($id === '') reply(422, ['ok'=>false,'error'=>'missing_id']); reply(200, locked(function(array $tasks) use ($id,$status,$by,$note): array { foreach ($tasks as $i => $t) { if (($t['id'] ?? '') !== $id) continue; $now = now_iso(); $tasks[$i]['status'] = $status; $tasks[$i]['updated_at'] = $now; if ($status === 'done' || $status === 'cancelled') $tasks[$i]['completed_at'] = $now; if ($status === 'queued') $tasks[$i]['claimed_at'] = null; $tasks[$i]['history'][] = ['at'=>$now,'event'=>$status,'by'=>$by,'note'=>$note]; return ['ok'=>true,'task'=>$tasks[$i],'__tasks'=>$tasks]; } return ['ok'=>false,'error'=>'not_found']; })); }
reply(404, ['ok'=>false,'error'=>'unknown_action']);
";
        }

        private static string CreatePhpCompat(string safeToken)
        {
            return @"<?php
const EASYMD_TOKEN = '" + safeToken + @"';
const EASYMD_VERSION = '1.3.0-compat';
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
if (!function_exists('http_response_code')) {
    function http_response_code($code = null) {
        static $current = 200;
        if ($code !== null) {
            $current = (int)$code;
            header('X-PHP-Response-Code: ' . $current, true, $current);
        }
        return $current;
    }
}
if (!function_exists('hash_equals')) {
    function hash_equals($known, $user) {
        if (!is_string($known) || !is_string($user) || strlen($known) !== strlen($user)) return false;
        $res = 0;
        for ($i = 0; $i < strlen($known); $i++) $res |= ord($known[$i]) ^ ord($user[$i]);
        return $res === 0;
    }
}
function easymd_reply($code, $data) {
    http_response_code($code);
    echo json_encode($data);
    exit;
}
function easymd_body() {
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') return array();
    $json = json_decode($raw, true);
    if (is_array($json)) return $json;
    $form = array();
    parse_str($raw, $form);
    return is_array($form) ? $form : array();
}
function easymd_req_token($body) {
    $headers = function_exists('getallheaders') ? getallheaders() : array();
    foreach ($headers as $name => $value) {
        if (strcasecmp((string)$name, 'X-EasyMD-Token') === 0) return trim((string)$value);
        if (strcasecmp((string)$name, 'Authorization') === 0) {
            $auth = trim((string)$value);
            if (stripos($auth, 'Bearer ') === 0) return trim(substr($auth, 7));
        }
    }
    if (isset($body['token'])) return trim((string)$body['token']);
    if (isset($_GET['token'])) return trim((string)$_GET['token']);
    return '';
}
function easymd_auth($body) {
    if (EASYMD_TOKEN === '' || !hash_equals(EASYMD_TOKEN, easymd_req_token($body))) {
        easymd_reply(401, array('ok'=>false, 'error'=>'unauthorized'));
    }
}
function easymd_data_dir() {
    $base = dirname(__FILE__);
    $candidates = array();
    $candidates[] = $base . DIRECTORY_SEPARATOR . '_data';
    $parent = dirname($base);
    if ($parent && $parent !== $base) $candidates[] = $parent . DIRECTORY_SEPARATOR . 'easymd_data';
    if (function_exists('sys_get_temp_dir')) {
        $tmp = sys_get_temp_dir();
        if ($tmp) $candidates[] = rtrim($tmp, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . 'easymd_' . substr(sha1($base . EASYMD_TOKEN), 0, 12);
    }
    foreach ($candidates as $dir) {
        if ((is_dir($dir) || @mkdir($dir, 0750, true)) && is_writable($dir)) {
            $probe = $dir . DIRECTORY_SEPARATOR . '.write-test';
            if (@file_put_contents($probe, 'ok') !== false) {
                @unlink($probe);
                $ht = $dir . DIRECTORY_SEPARATOR . '.htaccess';
                if (!file_exists($ht)) @file_put_contents($ht, ""Require all denied\nDeny from all\n"");
                return $dir;
            }
        }
    }
    easymd_reply(500, array(
        'ok'=>false,
        'error'=>'data_dir_unavailable',
        'message'=>'No se encontro ninguna carpeta escribible. Da permiso de escritura a la carpeta easymd o habilita sys_get_temp_dir().'
    ));
}
function easymd_tasks_file() { return easymd_data_dir() . DIRECTORY_SEPARATOR . 'tasks.json'; }
function easymd_lock_file() { return easymd_data_dir() . DIRECTORY_SEPARATOR . 'tasks.lock'; }
function easymd_now() { return gmdate('c'); }
function easymd_text($value, $fallback = '') {
    if (!is_scalar($value)) return $fallback;
    $text = trim((string)$value);
    return $text === '' ? $fallback : $text;
}
function easymd_status($value, $fallback = 'queued') {
    $status = easymd_text($value, $fallback);
    return in_array($status, array('queued','claimed','done','cancelled'), true) ? $status : $fallback;
}
function easymd_random_hex($length) {
    if (function_exists('random_bytes')) return bin2hex(random_bytes($length));
    if (function_exists('openssl_random_pseudo_bytes')) return bin2hex(openssl_random_pseudo_bytes($length));
    return substr(sha1(uniqid('', true) . mt_rand()), 0, $length * 2);
}
function easymd_load_tasks() {
    $file = easymd_tasks_file();
    if (!file_exists($file)) return array();
    $raw = file_get_contents($file);
    $json = $raw === false ? null : json_decode($raw, true);
    return is_array($json) ? array_values($json) : array();
}
function easymd_save_tasks($tasks) {
    $file = easymd_tasks_file();
    $tmp = $file . '.tmp';
    $json = json_encode(array_values($tasks));
    if ($json === false || file_put_contents($tmp, $json, LOCK_EX) === false || !rename($tmp, $file)) {
        @unlink($tmp);
        easymd_reply(500, array('ok'=>false, 'error'=>'write_failed', 'message'=>'No se pudo escribir tasks.json. Revisa permisos.'));
    }
}
function easymd_locked($callback) {
    $handle = fopen(easymd_lock_file(), 'c+');
    if ($handle === false || !flock($handle, LOCK_EX)) {
        easymd_reply(500, array('ok'=>false, 'error'=>'lock_failed', 'message'=>'No se pudo bloquear la cola. Revisa permisos.'));
    }
    $tasks = easymd_load_tasks();
    $result = call_user_func($callback, $tasks);
    if (isset($result['__tasks'])) {
        easymd_save_tasks($result['__tasks']);
        unset($result['__tasks']);
    }
    flock($handle, LOCK_UN);
    fclose($handle);
    return $result;
}
function easymd_task_value($task, $key, $fallback = '') {
    return isset($task[$key]) ? $task[$key] : $fallback;
}
function easymd_match_task($task, $filter) {
    foreach (array('project','target','source','status') as $key) {
        if (!isset($filter[$key]) || $filter[$key] === '') continue;
        if (easymd_task_value($task, $key) !== $filter[$key]) return false;
    }
    return true;
}
$body = easymd_body();
$action = easymd_text(isset($body['action']) ? $body['action'] : (isset($_GET['action']) ? $_GET['action'] : 'status'), 'status');
easymd_auth($body);
if ($action === 'status' || $action === 'ping') {
    easymd_reply(200, easymd_locked(function($tasks) {
        $counts = array('queued'=>0,'claimed'=>0,'done'=>0,'cancelled'=>0);
        foreach ($tasks as $task) $counts[easymd_status(easymd_task_value($task, 'status', 'queued'))]++;
        return array('ok'=>true, 'version'=>EASYMD_VERSION, 'counts'=>$counts);
    }));
}
if ($action === 'push') {
    $markdown = easymd_text(isset($body['markdown']) ? $body['markdown'] : (isset($body['text']) ? $body['text'] : ''));
    if ($markdown === '') easymd_reply(422, array('ok'=>false, 'error'=>'missing_markdown'));
    $now = easymd_now();
    $task = array(
        'id'=>'easymd-' . gmdate('Ymd-His') . '-' . easymd_random_hex(4),
        'created_at'=>$now,
        'updated_at'=>$now,
        'source'=>easymd_text(isset($body['source']) ? $body['source'] : 'unknown'),
        'target'=>easymd_text(isset($body['target']) ? $body['target'] : 'desktop'),
        'project'=>easymd_text(isset($body['project']) ? $body['project'] : 'APPbike'),
        'title'=>easymd_text(isset($body['title']) ? $body['title'] : 'Untitled handoff'),
        'priority'=>easymd_text(isset($body['priority']) ? $body['priority'] : 'normal'),
        'status'=>'queued',
        'claimed_at'=>null,
        'completed_at'=>null,
        'claim_count'=>0,
        'markdown'=>$markdown,
        'history'=>array(array('at'=>$now, 'event'=>'created', 'by'=>easymd_text(isset($body['source']) ? $body['source'] : 'unknown')))
    );
    easymd_reply(201, easymd_locked(function($tasks) use ($task) {
        $tasks[] = $task;
        return array('ok'=>true, 'task'=>$task, '__tasks'=>$tasks);
    }));
}
if ($action === 'list') {
    $filter = array(
        'project'=>easymd_text(isset($body['project']) ? $body['project'] : ''),
        'target'=>easymd_text(isset($body['target']) ? $body['target'] : ''),
        'source'=>easymd_text(isset($body['source']) ? $body['source'] : ''),
        'status'=>easymd_text(isset($body['status']) ? $body['status'] : '')
    );
    easymd_reply(200, easymd_locked(function($tasks) use ($filter) {
        $items = array();
        foreach ($tasks as $task) if (easymd_match_task($task, $filter)) $items[] = $task;
        usort($items, function($a, $b) { return strcmp(easymd_task_value($b, 'created_at'), easymd_task_value($a, 'created_at')); });
        return array('ok'=>true, 'tasks'=>array_slice($items, 0, 50));
    }));
}
if ($action === 'next') {
    $target = easymd_text(isset($body['target']) ? $body['target'] : 'desktop');
    $project = easymd_text(isset($body['project']) ? $body['project'] : 'APPbike');
    $claimer = easymd_text(isset($body['claimer']) ? $body['claimer'] : $target);
    easymd_reply(200, easymd_locked(function($tasks) use ($target, $project, $claimer) {
        foreach ($tasks as $i => $task) {
            if (easymd_task_value($task, 'target') === $target && easymd_task_value($task, 'project') === $project && easymd_task_value($task, 'status') === 'queued') {
                $now = easymd_now();
                $tasks[$i]['status'] = 'claimed';
                $tasks[$i]['claimed_at'] = $now;
                $tasks[$i]['updated_at'] = $now;
                $tasks[$i]['claim_count'] = (int)easymd_task_value($tasks[$i], 'claim_count', 0) + 1;
                $tasks[$i]['history'][] = array('at'=>$now, 'event'=>'claimed', 'by'=>$claimer);
                return array('ok'=>true, 'task'=>$tasks[$i], '__tasks'=>$tasks);
            }
        }
        return array('ok'=>true, 'task'=>null, 'message'=>'No queued task.');
    }));
}
if ($action === 'done' || $action === 'update') {
    $id = easymd_text(isset($body['id']) ? $body['id'] : '');
    $status = easymd_status(isset($body['status']) ? $body['status'] : ($action === 'done' ? 'done' : 'queued'));
    $by = easymd_text(isset($body['by']) ? $body['by'] : 'unknown');
    $note = easymd_text(isset($body['note']) ? $body['note'] : '');
    if ($id === '') easymd_reply(422, array('ok'=>false, 'error'=>'missing_id'));
    easymd_reply(200, easymd_locked(function($tasks) use ($id, $status, $by, $note) {
        foreach ($tasks as $i => $task) {
            if (easymd_task_value($task, 'id') !== $id) continue;
            $now = easymd_now();
            $tasks[$i]['status'] = $status;
            $tasks[$i]['updated_at'] = $now;
            if ($status === 'done' || $status === 'cancelled') $tasks[$i]['completed_at'] = $now;
            if ($status === 'queued') $tasks[$i]['claimed_at'] = null;
            $tasks[$i]['history'][] = array('at'=>$now, 'event'=>$status, 'by'=>$by, 'note'=>$note);
            return array('ok'=>true, 'task'=>$tasks[$i], '__tasks'=>$tasks);
        }
        return array('ok'=>false, 'error'=>'not_found');
    }));
}
easymd_reply(404, array('ok'=>false, 'error'=>'unknown_action'));
";
        }
    }

    internal sealed class SetupForm : Form
    {
        private readonly EasyConfig _config;
        private ComboBox _role;
        private TextBox _baseUrl;
        private TextBox _project;
        private RadioButton _createToken;
        private RadioButton _useToken;
        private TextBox[] _pairs;
        private TextBox _phraseA;
        private TextBox _phraseB;
        private TextBox _token;
        private CheckBox _autoRunCodex;
        private TextBox _codexCommand;
        private TextBox _codexArguments;
        private TextBox _workspaceDir;

        public SetupForm(EasyConfig config)
        {
            _config = config;
            Build();
        }

        private void Build()
        {
            Text = "Configurar EasyMD";
            StartPosition = FormStartPosition.CenterParent;
            Size = new Size(800, 780);
            MinimumSize = new Size(800, 780);
            BackColor = Color.FromArgb(246, 248, 244);
            Font = new Font("Segoe UI", 10);

            Label title = LabelText("EasyMD - Primer inicio", 24, 18, 680, 34, 18, FontStyle.Bold, Color.FromArgb(22, 74, 57));
            Controls.Add(title);
            Controls.Add(LabelText("Elige si este computador sera desktop o server, crea un token nuevo o pega uno existente.", 24, 58, 690, 28, 10, FontStyle.Regular, Color.FromArgb(76, 88, 78)));

            Controls.Add(LabelText("Rol", 24, 105, 80, 24, 10, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _role = new ComboBox { Left = 112, Top = 100, Width = 160, DropDownStyle = ComboBoxStyle.DropDownList };
            _role.Items.AddRange(new object[] { "desktop", "server" });
            _role.SelectedItem = string.IsNullOrWhiteSpace(_config.Role) ? "desktop" : _config.Role;
            Controls.Add(_role);

            Controls.Add(LabelText("Proyecto", 300, 105, 90, 24, 10, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _project = new TextBox { Left = 392, Top = 100, Width = 150, Text = string.IsNullOrWhiteSpace(_config.Project) ? "APPbike" : _config.Project };
            Controls.Add(_project);

            Controls.Add(LabelText("URL", 24, 143, 80, 24, 10, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _baseUrl = new TextBox { Left = 112, Top = 138, Width = 520, Text = string.IsNullOrWhiteSpace(_config.BaseUrl) ? "https://api.zizzio.cl/easymd" : _config.BaseUrl };
            Controls.Add(_baseUrl);

            _createToken = new RadioButton { Left = 24, Top = 190, Width = 230, Text = "Crear token nuevo", Checked = string.IsNullOrWhiteSpace(_config.Token) };
            _useToken = new RadioButton { Left = 270, Top = 190, Width = 230, Text = "Usar token existente", Checked = !string.IsNullOrWhiteSpace(_config.Token) };
            Controls.Add(_createToken);
            Controls.Add(_useToken);

            _pairs = new TextBox[5];
            for (int i = 0; i < 5; i++)
            {
                int left = 24 + (i * 92);
                Controls.Add(LabelText("Par " + (i + 1), left, 232, 70, 22, 9, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
                _pairs[i] = new TextBox { Left = left, Top = 256, Width = 60, MaxLength = 2 };
                Controls.Add(_pairs[i]);
            }

            Controls.Add(LabelText("Frase A (8 caracteres)", 24, 305, 190, 22, 9, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _phraseA = new TextBox { Left = 24, Top = 330, Width = 220, MaxLength = 8 };
            Controls.Add(_phraseA);

            Controls.Add(LabelText("Frase B (8 caracteres)", 270, 305, 190, 22, 9, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _phraseB = new TextBox { Left = 270, Top = 330, Width = 220, MaxLength = 8 };
            Controls.Add(_phraseB);

            Button generate = ButtonText("Crear token", 520, 326, 140, 34);
            generate.Click += delegate { GenerateToken(); };
            Controls.Add(generate);

            Controls.Add(LabelText("Token", 24, 385, 90, 22, 10, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _token = new TextBox { Left = 24, Top = 410, Width = 610, Height = 78, Multiline = true, ScrollBars = ScrollBars.Vertical, Text = _config.Token ?? "" };
            Controls.Add(_token);
            Button copy = ButtonText("Copiar", 645, 410, 80, 34);
            copy.Click += delegate { if (!string.IsNullOrWhiteSpace(_token.Text)) Clipboard.SetText(_token.Text); };
            Controls.Add(copy);

            _autoRunCodex = new CheckBox { Left = 24, Top = 505, Width = 320, Height = 24, Text = "Ejecutar Codex al recibir tarea", Checked = _config.AutoRunCodex };
            Controls.Add(_autoRunCodex);

            Controls.Add(LabelText("Comando Codex", 24, 540, 140, 22, 9, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            string detectedCodex = string.IsNullOrWhiteSpace(_config.CodexCommand) || _config.CodexCommand == "codex" ? Store.FindCodexCommand() : _config.CodexCommand;
            _codexCommand = new TextBox { Left = 165, Top = 536, Width = 170, Text = detectedCodex };
            Controls.Add(_codexCommand);
            Button browseCodex = ButtonText("...", 340, 536, 36, 26);
            browseCodex.Click += delegate { BrowseCodex(); };
            Controls.Add(browseCodex);

            Controls.Add(LabelText("Carpeta trabajo", 395, 540, 130, 22, 9, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _workspaceDir = new TextBox { Left = 525, Top = 536, Width = 180, Text = string.IsNullOrWhiteSpace(_config.WorkspaceDir) ? Environment.CurrentDirectory : _config.WorkspaceDir };
            Controls.Add(_workspaceDir);
            Button browseWorkspace = ButtonText("...", 710, 536, 36, 26);
            browseWorkspace.Click += delegate { BrowseWorkspace(); };
            Controls.Add(browseWorkspace);

            Controls.Add(LabelText("Argumentos. Variables: {file}, {workspace}, {id}, {title}, {role}, {project}", 24, 578, 720, 22, 9, FontStyle.Bold, Color.FromArgb(36, 51, 43)));
            _codexArguments = new TextBox { Left = 24, Top = 604, Width = 720, Height = 54, Multiline = true, ScrollBars = ScrollBars.Vertical, Text = string.IsNullOrWhiteSpace(_config.CodexArguments) ? EasyConfig.DefaultCodexArguments : _config.CodexArguments };
            Controls.Add(_codexArguments);

            Button save = ButtonText("Guardar configuracion", 24, 688, 220, 42);
            save.Click += delegate { SaveAndClose(); };
            Controls.Add(save);

            Button index = ButtonText("Guardar index.php", 270, 688, 180, 42);
            index.Click += delegate { SaveEndpoint(); };
            Controls.Add(index);

            Button cancel = ButtonText("Cerrar", 625, 688, 120, 42);
            cancel.Click += delegate { DialogResult = DialogResult.Cancel; Close(); };
            Controls.Add(cancel);
        }

        private static Label LabelText(string text, int left, int top, int width, int height, int size, FontStyle style, Color color)
        {
            return new Label { Text = text, Left = left, Top = top, Width = width, Height = height, Font = new Font("Segoe UI", size, style), ForeColor = color };
        }

        private static Button ButtonText(string text, int left, int top, int width, int height)
        {
            Button b = new Button { Text = text, Left = left, Top = top, Width = width, Height = height, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(38, 133, 93), ForeColor = Color.White };
            b.FlatAppearance.BorderSize = 0;
            return b;
        }

        private void GenerateToken()
        {
            string[] pairs = new string[5];
            for (int i = 0; i < 5; i++)
            {
                pairs[i] = _pairs[i].Text.Trim();
                int number;
                if (pairs[i].Length != 2 || !int.TryParse(pairs[i], out number))
                {
                    MessageBox.Show("Cada par debe tener exactamente 2 numeros.", "EasyMD", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                    return;
                }
            }
            if (_phraseA.Text.Length != 8 || _phraseB.Text.Length != 8)
            {
                MessageBox.Show("Cada frase debe tener exactamente 8 caracteres.", "EasyMD", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }
            _token.Text = TokenFactory.Create(pairs, _phraseA.Text, _phraseB.Text);
        }

        private void SaveEndpoint()
        {
            if (string.IsNullOrWhiteSpace(_token.Text))
            {
                MessageBox.Show("Primero crea o pega un token.", "EasyMD", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }
            using (SaveFileDialog dialog = new SaveFileDialog())
            {
                dialog.FileName = "index.php";
                dialog.Filter = "PHP (*.php)|*.php|Todos (*.*)|*.*";
                if (dialog.ShowDialog(this) != DialogResult.OK) return;
                File.WriteAllText(dialog.FileName, EndpointFactory.CreatePhp(_token.Text.Trim()), Encoding.UTF8);
                MessageBox.Show("index.php listo. Subelo a api.zizzio.cl/easymd.", "EasyMD", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
        }

        private void SaveAndClose()
        {
            if (string.IsNullOrWhiteSpace(_token.Text))
            {
                MessageBox.Show("Primero crea o pega un token.", "EasyMD", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }
            _config.Role = Convert.ToString(_role.SelectedItem);
            _config.Token = _token.Text.Trim();
            _config.BaseUrl = _baseUrl.Text.Trim().TrimEnd('/');
            _config.Project = _project.Text.Trim();
            _config.AutoRunCodex = _autoRunCodex.Checked;
            _config.CodexCommand = _codexCommand.Text.Trim();
            _config.CodexArguments = _codexArguments.Text.Trim();
            _config.WorkspaceDir = _workspaceDir.Text.Trim();
            Store.Save(_config);
            DialogResult = DialogResult.OK;
            Close();
        }

        private void BrowseWorkspace()
        {
            using (FolderBrowserDialog dialog = new FolderBrowserDialog())
            {
                dialog.Description = "Carpeta donde Codex debe trabajar";
                dialog.SelectedPath = Directory.Exists(_workspaceDir.Text) ? _workspaceDir.Text : Environment.CurrentDirectory;
                if (dialog.ShowDialog(this) == DialogResult.OK)
                {
                    _workspaceDir.Text = dialog.SelectedPath;
                }
            }
        }

        private void BrowseCodex()
        {
            using (OpenFileDialog dialog = new OpenFileDialog())
            {
                dialog.Title = "Seleccionar codex.exe";
                dialog.Filter = "Codex (codex.exe)|codex.exe|Ejecutables (*.exe)|*.exe|Todos (*.*)|*.*";
                if (File.Exists(_codexCommand.Text))
                {
                    dialog.InitialDirectory = Path.GetDirectoryName(_codexCommand.Text);
                    dialog.FileName = Path.GetFileName(_codexCommand.Text);
                }
                if (dialog.ShowDialog(this) == DialogResult.OK)
                {
                    _codexCommand.Text = dialog.FileName;
                }
            }
        }
    }

    internal sealed class MainForm : Form
    {
        private EasyConfig _config;
        private Label _status;
        private TextBox _title;
        private TextBox _markdown;
        private TextBox _taskId;
        private TextBox _output;

        public MainForm()
        {
            _config = Store.Load();
            Build();
            Shown += delegate
            {
                if (string.IsNullOrWhiteSpace(_config.Token) || string.IsNullOrWhiteSpace(_config.Role))
                {
                    OpenSetup();
                }
                RefreshStatus(false);
            };
        }

        private void Build()
        {
            Text = "EasyMD - puente Codex";
            StartPosition = FormStartPosition.CenterScreen;
            Size = new Size(1060, 760);
            MinimumSize = new Size(980, 680);
            BackColor = Color.FromArgb(239, 244, 237);
            Font = new Font("Segoe UI", 10);

            Panel header = new Panel { Left = 0, Top = 0, Width = 1060, Height = 90, BackColor = Color.FromArgb(21, 88, 63), Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right };
            Controls.Add(header);
            header.Controls.Add(new Label { Text = "EasyMD", Left = 28, Top = 18, Width = 220, Height = 34, ForeColor = Color.White, Font = new Font("Segoe UI", 20, FontStyle.Bold) });
            header.Controls.Add(new Label { Text = "Relevo automatico entre Codex del servidor y del PC principal", Left = 30, Top = 54, Width = 650, Height = 24, ForeColor = Color.FromArgb(217, 236, 223), Font = new Font("Segoe UI", 10) });
            _status = new Label { Text = "", Left = 730, Top = 24, Width = 300, Height = 42, ForeColor = Color.White, TextAlign = ContentAlignment.MiddleRight, Anchor = AnchorStyles.Top | AnchorStyles.Right };
            header.Controls.Add(_status);

            Panel left = Card(24, 112, 312, 580);
            Controls.Add(left);
            left.Controls.Add(TitleLabel("Control", 18, 16));
            AddButton(left, "Configurar / token", 18, 60, 260, OpenSetup);
            AddButton(left, "Generar index.php", 18, 112, 260, SaveEndpoint);
            AddButton(left, "Iniciar fondo", 18, 184, 124, StartWatcher);
            AddButton(left, "Detener fondo", 154, 184, 124, StopWatcher);
            AddButton(left, "Estado", 18, 236, 124, delegate { RefreshStatus(true); });
            AddButton(left, "Listar cola", 154, 236, 124, ListQueue);
            left.Controls.Add(SmallLabel("ID para marcar terminada", 18, 312, 250));
            _taskId = new TextBox { Left = 18, Top = 340, Width = 260 };
            left.Controls.Add(_taskId);
            AddButton(left, "Marcar terminada", 18, 386, 260, DoneTask);
            AddButton(left, "Reponer a cola", 18, 430, 260, RequeueTask);
            AddButton(left, "Abrir carpeta inbox", 18, 474, 260, OpenInbox);
            AddButton(left, "Abrir logs", 18, 518, 124, OpenLogs);
            AddButton(left, "Probar Codex", 154, 518, 124, TestCodex);

            Panel right = Card(360, 112, 660, 580);
            right.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(right);
            right.Controls.Add(TitleLabel("Tareas", 18, 16));
            right.Controls.Add(SmallLabel("Titulo", 18, 60, 120));
            _title = new TextBox { Left = 18, Top = 86, Width = 600, Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right, Text = "Siguiente trabajo APPbike" };
            right.Controls.Add(_title);
            right.Controls.Add(SmallLabel("Markdown / instrucciones", 18, 125, 260));
            _markdown = new TextBox { Left = 18, Top = 152, Width = 600, Height = 160, Multiline = true, ScrollBars = ScrollBars.Vertical, Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right };
            right.Controls.Add(_markdown);
            AddButton(right, "Enviar al otro Codex", 18, 326, 180, SendTask);
            AddButton(right, "Recibir siguiente", 214, 326, 160, NextTask);
            AddButton(right, "Cargar .md", 390, 326, 120, LoadMarkdown);
            right.Controls.Add(SmallLabel("Salida", 18, 382, 120));
            _output = new TextBox { Left = 18, Top = 408, Width = 600, Height = 140, Multiline = true, ScrollBars = ScrollBars.Vertical, ReadOnly = true, Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right, BackColor = Color.White };
            right.Controls.Add(_output);
        }

        private static Panel Card(int left, int top, int width, int height)
        {
            return new Panel { Left = left, Top = top, Width = width, Height = height, BackColor = Color.White, BorderStyle = BorderStyle.FixedSingle };
        }

        private static Label TitleLabel(string text, int left, int top)
        {
            return new Label { Text = text, Left = left, Top = top, Width = 250, Height = 32, Font = new Font("Segoe UI", 15, FontStyle.Bold), ForeColor = Color.FromArgb(30, 72, 55) };
        }

        private static Label SmallLabel(string text, int left, int top, int width)
        {
            return new Label { Text = text, Left = left, Top = top, Width = width, Height = 24, Font = new Font("Segoe UI", 9, FontStyle.Bold), ForeColor = Color.FromArgb(70, 82, 74) };
        }

        private static void AddButton(Control parent, string text, int left, int top, int width, Action action)
        {
            Button b = new Button { Text = text, Left = left, Top = top, Width = width, Height = 38, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(38, 133, 93), ForeColor = Color.White };
            b.FlatAppearance.BorderSize = 0;
            b.Click += delegate { try { action(); } catch (Exception ex) { MessageBox.Show(FriendlyError(ex), "EasyMD", MessageBoxButtons.OK, MessageBoxIcon.Error); } };
            parent.Controls.Add(b);
        }

        private static string FriendlyError(Exception ex)
        {
            if (!string.IsNullOrWhiteSpace(ex.Message)) return ex.Message;
            if (ex.InnerException != null && !string.IsNullOrWhiteSpace(ex.InnerException.Message)) return ex.InnerException.Message;
            return ex.GetType().FullName;
        }

        private void OpenSetup()
        {
            using (SetupForm form = new SetupForm(_config))
            {
                form.ShowDialog(this);
            }
            _config = Store.Load();
            RefreshStatus(false);
        }

        private void SaveEndpoint()
        {
            if (string.IsNullOrWhiteSpace(_config.Token)) throw new InvalidOperationException("Configura o crea un token primero.");
            using (SaveFileDialog dialog = new SaveFileDialog())
            {
                dialog.FileName = "index.php";
                dialog.Filter = "PHP (*.php)|*.php|Todos (*.*)|*.*";
                if (dialog.ShowDialog(this) != DialogResult.OK) return;
                File.WriteAllText(dialog.FileName, EndpointFactory.CreatePhp(_config.Token), Encoding.UTF8);
                WriteLine("index.php creado: " + dialog.FileName);
            }
        }

        private void StartWatcher()
        {
            if (!_config.AutoRunCodex)
            {
                WriteLine("Aviso: auto-Codex esta desactivado. El monitor no reclamara tareas hasta activarlo en Configurar / token.");
            }
            Process p = GetWatchProcess();
            if (p != null)
            {
                WriteLine("El monitor ya esta corriendo. PID " + p.Id);
                return;
            }
            ProcessStartInfo psi = new ProcessStartInfo(Application.ExecutablePath, "--watch");
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.WindowStyle = ProcessWindowStyle.Hidden;
            Process started = Process.Start(psi);
            File.WriteAllText(Store.PidPath, started.Id.ToString(), Encoding.ASCII);
            WriteLine("Monitor iniciado. PID " + started.Id);
            RefreshStatus(false);
        }

        private void StopWatcher()
        {
            Process p = GetWatchProcess();
            if (p == null)
            {
                WriteLine("El monitor no esta corriendo.");
                return;
            }
            p.Kill();
            try { File.Delete(Store.PidPath); } catch { }
            WriteLine("Monitor detenido. PID " + p.Id);
            RefreshStatus(false);
        }

        private void RefreshStatus(bool callServer)
        {
            Process p = GetWatchProcess();
            _status.Text = "Rol: " + (_config.Role == "" ? "sin configurar" : _config.Role) + Environment.NewLine +
                           "Fondo: " + (p == null ? "detenido" : "PID " + p.Id);
            if (!callServer) return;
            Dictionary<string, object> r = EasyApi.Call(_config, new Dictionary<string, object> { { "action", "status" } });
            WriteLine("Servidor OK. Version: " + Value(r, "version"));
            if (r.ContainsKey("counts"))
            {
                Dictionary<string, object> c = (Dictionary<string, object>)r["counts"];
                WriteLine("Cola: queued=" + Value(c, "queued") + ", claimed=" + Value(c, "claimed") + ", done=" + Value(c, "done") + ", cancelled=" + Value(c, "cancelled"));
            }
        }

        private void SendTask()
        {
            string destination = _config.Role == "desktop" ? "server" : "desktop";
            Dictionary<string, object> r = EasyApi.Call(_config, new Dictionary<string, object>
            {
                { "action", "push" },
                { "source", _config.Role },
                { "target", destination },
                { "project", _config.Project },
                { "title", _title.Text },
                { "markdown", _markdown.Text }
            });
            Dictionary<string, object> task = (Dictionary<string, object>)r["task"];
            WriteLine("Tarea enviada a " + destination + ": " + Value(task, "id"));
        }

        private void NextTask()
        {
            Dictionary<string, object> r = EasyApi.Call(_config, new Dictionary<string, object>
            {
                { "action", "next" },
                { "target", _config.Role },
                { "project", _config.Project },
                { "claimer", _config.Role }
            });
            if (r["task"] == null)
            {
                WriteLine("No hay tareas pendientes.");
                return;
            }
            Dictionary<string, object> task = (Dictionary<string, object>)r["task"];
            string path = Watcher.SaveTask(_config, task);
            _taskId.Text = Value(task, "id");
            _markdown.Text = Value(task, "markdown");
            WriteLine("Tarea recibida y guardada: " + path);
            if (TaskProcessor.IsResponse(task))
            {
                TaskProcessor.CompleteReceivedResponse(_config, task, WriteLine);
                return;
            }
            if (_config.AutoRunCodex)
            {
                RunRemoteTaskInBackground(task, path);
            }
        }

        private void DoneTask()
        {
            if (string.IsNullOrWhiteSpace(_taskId.Text)) throw new InvalidOperationException("Pega o escribe el ID de tarea.");
            Dictionary<string, object> r = EasyApi.Call(_config, new Dictionary<string, object>
            {
                { "action", "done" },
                { "id", _taskId.Text.Trim() },
                { "status", "done" },
                { "by", _config.Role },
                { "note", "" }
            });
            Dictionary<string, object> task = (Dictionary<string, object>)r["task"];
            WriteLine("Tarea terminada: " + Value(task, "id"));
        }

        private void RequeueTask()
        {
            if (string.IsNullOrWhiteSpace(_taskId.Text)) throw new InvalidOperationException("Pega o escribe el ID de tarea.");
            Dictionary<string, object> r = EasyApi.Call(_config, new Dictionary<string, object>
            {
                { "action", "update" },
                { "id", _taskId.Text.Trim() },
                { "status", "queued" },
                { "by", _config.Role },
                { "note", "Repuesta manualmente a cola desde EasyMD." }
            });
            Dictionary<string, object> task = (Dictionary<string, object>)r["task"];
            WriteLine("Tarea repuesta a cola: " + Value(task, "id"));
        }

        private void ListQueue()
        {
            Dictionary<string, object> r = EasyApi.Call(_config, new Dictionary<string, object>
            {
                { "action", "list" },
                { "project", _config.Project },
                { "target", _config.Role }
            });
            WriteLine("Tareas para " + _config.Role + ":");
            IEnumerable tasks = r["tasks"] as IEnumerable;
            if (tasks == null)
            {
                WriteLine("La respuesta no incluyo lista de tareas.");
                return;
            }
            foreach (object item in tasks)
            {
                Dictionary<string, object> task = (Dictionary<string, object>)item;
                WriteLine(Value(task, "id") + " | " + Value(task, "status") + " | " + Value(task, "title"));
            }
        }

        private void LoadMarkdown()
        {
            using (OpenFileDialog dialog = new OpenFileDialog())
            {
                dialog.Filter = "Markdown (*.md)|*.md|Texto (*.txt)|*.txt|Todos (*.*)|*.*";
                if (dialog.ShowDialog(this) != DialogResult.OK) return;
                _markdown.Text = File.ReadAllText(dialog.FileName, Encoding.UTF8);
            }
        }

        private void OpenInbox()
        {
            Directory.CreateDirectory(_config.OutDir);
            Process.Start("explorer.exe", _config.OutDir);
        }

        private void OpenLogs()
        {
            Directory.CreateDirectory(Store.Dir);
            Process.Start("explorer.exe", Store.Dir);
        }

        private void TestCodex()
        {
            Dictionary<string, object> task = new Dictionary<string, object>();
            task["id"] = "easymd-test-" + DateTime.UtcNow.ToString("yyyyMMdd-HHmmss");
            task["title"] = "Prueba local de Codex";
            task["source"] = _config.Role;
            task["target"] = _config.Role;
            task["project"] = _config.Project;
            task["created_at"] = DateTime.UtcNow.ToString("o");
            task["markdown"] = "# Prueba local EasyMD\n\nConfirma que Codex puede ejecutarse desde EasyMD.";
            string path = Watcher.SaveTask(_config, task);
            EasyConfig testConfig = _config;
            WriteLine("Prueba local iniciada. EasyMD esperara y capturara la respuesta final de Codex.");
            ThreadPool.QueueUserWorkItem(delegate
            {
                CodexRunResult result = CodexRunner.RunAndWait(testConfig, task, path);
                UiWriteLine(result.Success
                    ? "Prueba Codex OK. Respuesta: " + result.FinalMessage
                    : "Prueba Codex fallo: " + result.ErrorMessage);
                UiWriteLine("Logs: " + result.StdoutPath + " | " + result.StderrPath);
            });
        }

        private void RunRemoteTaskInBackground(Dictionary<string, object> task, string path)
        {
            EasyConfig runConfig = _config;
            WriteLine("Codex iniciado en segundo plano. EasyMD enviara la respuesta y cerrara la tarea al finalizar.");
            ThreadPool.QueueUserWorkItem(delegate
            {
                try
                {
                    TaskProcessor.ProcessRemoteTask(runConfig, task, path, UiWriteLine);
                }
                catch (Exception ex)
                {
                    UiWriteLine("La tarea quedo claimed porque no se pudo completar el relevo: " + FriendlyError(ex));
                }
            });
        }

        private void UiWriteLine(string line)
        {
            if (IsDisposed || Disposing) return;
            if (InvokeRequired)
            {
                try { BeginInvoke(new Action<string>(UiWriteLine), line); } catch { }
                return;
            }
            WriteLine(line);
        }

        private static Process GetWatchProcess()
        {
            if (!File.Exists(Store.PidPath)) return null;
            int pid;
            if (!int.TryParse(File.ReadAllText(Store.PidPath).Trim(), out pid)) return null;
            try { return Process.GetProcessById(pid); } catch { return null; }
        }

        private static string Value(Dictionary<string, object> dict, string key)
        {
            return dict.ContainsKey(key) && dict[key] != null ? Convert.ToString(dict[key]) : "";
        }

        private void WriteLine(string line)
        {
            _output.AppendText("[" + DateTime.Now.ToString("HH:mm:ss") + "] " + line + Environment.NewLine);
        }
    }

    internal sealed class CodexRunResult
    {
        public bool Success;
        public int ExitCode = -1;
        public string FinalMessage = "";
        public string ErrorMessage = "";
        public string StdoutPath = "";
        public string StderrPath = "";
        public string FinalMessagePath = "";
    }

    internal static class TaskProcessor
    {
        public static bool IsResponse(Dictionary<string, object> task)
        {
            return Value(task, "priority").Equals("response", StringComparison.OrdinalIgnoreCase)
                || Value(task, "title").StartsWith("Respuesta EasyMD a ", StringComparison.Ordinal);
        }

        public static void CompleteReceivedResponse(EasyConfig cfg, Dictionary<string, object> task, Action<string> log)
        {
            string taskId = Value(task, "id");
            Dictionary<string, object> done = EasyApi.Call(cfg, new Dictionary<string, object>
            {
                { "action", "done" },
                { "id", taskId },
                { "status", "done" },
                { "by", cfg.Role },
                { "note", "Respuesta recibida y guardada por EasyMD; no requiere otra ejecucion de Codex." }
            });
            EnsureOk(done, "cerrar la respuesta recibida");
            log("Respuesta recibida y guardada sin crear otro ciclo Codex: " + taskId);
        }

        public static void ProcessRemoteTask(EasyConfig cfg, Dictionary<string, object> task, string taskPath, Action<string> log)
        {
            string taskId = Value(task, "id");
            CodexRunResult result = CodexRunner.RunAndWait(cfg, task, taskPath);
            log(result.Success
                ? "Codex termino correctamente para " + taskId + "."
                : "Codex termino con error para " + taskId + ": " + result.ErrorMessage);

            string target = Value(task, "source");
            if (string.IsNullOrWhiteSpace(target)) target = cfg.Role == "desktop" ? "server" : "desktop";
            string project = Value(task, "project");
            if (string.IsNullOrWhiteSpace(project)) project = cfg.Project;

            StringBuilder markdown = new StringBuilder();
            markdown.AppendLine("# Respuesta automatica EasyMD");
            markdown.AppendLine();
            markdown.AppendLine("Tarea original: " + taskId);
            markdown.AppendLine("Respondido por: " + cfg.Role);
            markdown.AppendLine("Resultado Codex: " + (result.Success ? "correcto" : "error"));
            markdown.AppendLine("Codigo de salida: " + result.ExitCode);
            markdown.AppendLine();
            markdown.AppendLine(result.Success ? result.FinalMessage : result.ErrorMessage);

            Dictionary<string, object> reply = EasyApi.Call(cfg, new Dictionary<string, object>
            {
                { "action", "push" },
                { "source", cfg.Role },
                { "target", target },
                { "project", project },
                { "title", "Respuesta EasyMD a " + taskId },
                { "priority", "response" },
                { "markdown", markdown.ToString() }
            });
            EnsureOk(reply, "enviar la respuesta");
            Dictionary<string, object> replyTask = reply.ContainsKey("task") ? reply["task"] as Dictionary<string, object> : null;
            log("Respuesta enviada a " + target + ": " + (replyTask == null ? "ID no informado" : Value(replyTask, "id")));

            Dictionary<string, object> done = EasyApi.Call(cfg, new Dictionary<string, object>
            {
                { "action", "done" },
                { "id", taskId },
                { "status", "done" },
                { "by", cfg.Role },
                { "note", result.Success ? "Codex finalizo y EasyMD envio la respuesta automatica." : "EasyMD informo automaticamente el error de Codex." }
            });
            EnsureOk(done, "marcar la tarea como terminada");
            log("Tarea original terminada: " + taskId);
        }

        private static void EnsureOk(Dictionary<string, object> response, string action)
        {
            if (response == null || !response.ContainsKey("ok") || !Convert.ToBoolean(response["ok"]))
            {
                string error = response != null && response.ContainsKey("error") ? Convert.ToString(response["error"]) : "respuesta invalida";
                throw new InvalidOperationException("No se pudo " + action + ": " + error);
            }
        }

        private static string Value(Dictionary<string, object> dict, string key)
        {
            return dict.ContainsKey(key) && dict[key] != null ? Convert.ToString(dict[key]) : "";
        }
    }

    internal static class CodexRunner
    {
        private static readonly object LogSync = new object();

        public static CodexRunResult RunAndWait(EasyConfig cfg, Dictionary<string, object> task, string taskPath)
        {
            CodexRunResult result = new CodexRunResult();
            if (string.IsNullOrWhiteSpace(cfg.CodexCommand))
            {
                result.ErrorMessage = "Falta el comando Codex.";
                return result;
            }

            string id = Value(task, "id");
            string title = Value(task, "title");
            string workspace = string.IsNullOrWhiteSpace(cfg.WorkspaceDir) ? Environment.CurrentDirectory : cfg.WorkspaceDir;
            Directory.CreateDirectory(Store.Dir);
            string runDir = Path.Combine(Store.Dir, "codex-runs");
            Directory.CreateDirectory(runDir);
            string stdout = Path.Combine(runDir, id + ".out.log");
            string stderr = Path.Combine(runDir, id + ".err.log");
            string finalMessage = Path.Combine(runDir, id + "-" + DateTime.UtcNow.ToString("yyyyMMddHHmmssfff") + ".final.md");
            result.StdoutPath = stdout;
            result.StderrPath = stderr;
            result.FinalMessagePath = finalMessage;

            string args = string.IsNullOrWhiteSpace(cfg.CodexArguments)
                ? EasyConfig.DefaultCodexArguments
                : cfg.CodexArguments;

            args = ApplyTemplate(args, cfg, taskPath, id, title, workspace);
            args = EnsureFinalMessageOutput(args, finalMessage);
            string configuredCommand = ApplyTemplate(cfg.CodexCommand, cfg, taskPath, id, title, workspace).Trim();
            string command = ResolveCommand(configuredCommand);
            AppendLine(stdout, "EasyMD starting Codex");
            AppendLine(stdout, "Configured command: " + configuredCommand);
            AppendLine(stdout, "Resolved command: " + command);
            AppendLine(stdout, "Arguments: " + args);
            AppendLine(stdout, "Workspace: " + workspace);

            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(command, args);
                psi.WorkingDirectory = Directory.Exists(workspace) ? workspace : Environment.CurrentDirectory;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError = true;

                using (Process process = new Process())
                {
                    process.StartInfo = psi;
                    process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e)
                    {
                        if (e.Data != null) AppendLine(stdout, e.Data);
                    };
                    process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e)
                    {
                        if (e.Data != null) AppendLine(stderr, e.Data);
                    };
                    process.Start();
                    AppendLine(stdout, "EasyMD Codex PID " + process.Id);
                    process.BeginOutputReadLine();
                    process.BeginErrorReadLine();
                    process.WaitForExit();
                    process.WaitForExit();
                    result.ExitCode = process.ExitCode;
                }

                result.FinalMessage = File.Exists(finalMessage) ? File.ReadAllText(finalMessage, Encoding.UTF8).Trim() : "";
                result.Success = result.ExitCode == 0 && !string.IsNullOrWhiteSpace(result.FinalMessage);
                if (!result.Success)
                {
                    result.ErrorMessage = result.ExitCode == 0
                        ? "Codex termino sin producir un mensaje final. Revisa " + stderr
                        : "Codex termino con codigo " + result.ExitCode + ". Revisa " + stderr;
                }
                AppendLine(stdout, "EasyMD Codex process exited with code " + result.ExitCode);
                return result;
            }
            catch (Exception ex)
            {
                result.ErrorMessage = "No se pudo ejecutar Codex: " + ex.Message;
                AppendLine(stderr, result.ErrorMessage);
                return result;
            }
        }

        private static string EnsureFinalMessageOutput(string args, string resultPath)
        {
            if (args.IndexOf("--output-last-message", StringComparison.OrdinalIgnoreCase) >= 0) return args;
            if ((" " + args + " ").IndexOf(" -o ", StringComparison.OrdinalIgnoreCase) >= 0) return args;
            string trimmed = args.TrimStart();
            int separator = trimmed.IndexOf(' ');
            string command = separator < 0 ? trimmed : trimmed.Substring(0, separator);
            if (!command.Equals("exec", StringComparison.OrdinalIgnoreCase) && !command.Equals("e", StringComparison.OrdinalIgnoreCase))
            {
                return args;
            }
            string remainder = separator < 0 ? "" : trimmed.Substring(separator + 1);
            return command + " --output-last-message \"" + resultPath.Replace("\"", "\\\"") + "\" " + remainder;
        }

        private static string ApplyTemplate(string value, EasyConfig cfg, string file, string id, string title, string workspace)
        {
            return value
                .Replace("{file}", file)
                .Replace("{workspace}", workspace)
                .Replace("{id}", id)
                .Replace("{title}", title.Replace("\"", "'"))
                .Replace("{role}", cfg.Role)
                .Replace("{project}", cfg.Project);
        }

        private static string ResolveCommand(string configuredCommand)
        {
            string command = configuredCommand.Trim('"');
            if (string.IsNullOrWhiteSpace(command)) return Store.FindCodexCommand();
            if (Path.IsPathRooted(command) && File.Exists(command)) return command;
            if (File.Exists(command)) return Path.GetFullPath(command);
            if (command.Equals("codex", StringComparison.OrdinalIgnoreCase) || command.Equals("codex.exe", StringComparison.OrdinalIgnoreCase))
            {
                string found = Store.FindCodexCommand();
                if (!string.IsNullOrWhiteSpace(found)) return found;
            }
            string onPath = Store.FindOnPath(command.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) ? command : command + ".exe");
            if (!string.IsNullOrWhiteSpace(onPath)) return onPath;
            return configuredCommand;
        }

        private static void AppendLine(string path, string line)
        {
            try
            {
                lock (LogSync)
                {
                    File.AppendAllText(path, "[" + DateTime.Now.ToString("s") + "] " + line + Environment.NewLine, Encoding.UTF8);
                }
            }
            catch { }
        }

        private static string Value(Dictionary<string, object> dict, string key)
        {
            return dict.ContainsKey(key) && dict[key] != null ? Convert.ToString(dict[key]) : "";
        }
    }

    internal static class Watcher
    {
        public static void Run()
        {
            while (true)
            {
                try
                {
                    EasyConfig cfg = Store.Load();
                    if (!cfg.AutoRunCodex)
                    {
                        Log("Auto-Codex desactivado. El monitor no reclamara tareas.");
                        Thread.Sleep(Math.Max(10, cfg.IntervalSeconds) * 1000);
                        continue;
                    }
                    Dictionary<string, object> response = EasyApi.Call(cfg, new Dictionary<string, object>
                    {
                        { "action", "next" },
                        { "target", cfg.Role },
                        { "project", cfg.Project },
                        { "claimer", cfg.Role }
                    });
                    if (response.ContainsKey("task") && response["task"] != null)
                    {
                        Dictionary<string, object> task = (Dictionary<string, object>)response["task"];
                        string path = SaveTask(cfg, task);
                        Log("Nueva tarea guardada: " + path);
                        if (TaskProcessor.IsResponse(task))
                        {
                            TaskProcessor.CompleteReceivedResponse(cfg, task, Log);
                        }
                        else
                        {
                            TaskProcessor.ProcessRemoteTask(cfg, task, path, Log);
                        }
                    }
                    else
                    {
                        Log("Sin tareas.");
                    }
                    Thread.Sleep(Math.Max(10, cfg.IntervalSeconds) * 1000);
                }
                catch (Exception ex)
                {
                    Log("Error: " + ex.Message);
                    Thread.Sleep(60000);
                }
            }
        }

        public static string SaveTask(EasyConfig cfg, Dictionary<string, object> task)
        {
            Directory.CreateDirectory(cfg.OutDir);
            string id = Convert.ToString(task["id"]);
            string title = Convert.ToString(task["title"]);
            string safeTitle = SafeFileName(title);
            string path = Path.Combine(cfg.OutDir, id + "-" + safeTitle + ".md");
            StringBuilder text = new StringBuilder();
            text.AppendLine("# " + title);
            text.AppendLine();
            text.AppendLine("Task: " + id);
            text.AppendLine("Source: " + Value(task, "source"));
            text.AppendLine("Target: " + Value(task, "target"));
            text.AppendLine("Project: " + Value(task, "project"));
            text.AppendLine("Created: " + Value(task, "created_at"));
            text.AppendLine("## Instruccion para Codex");
            text.AppendLine();
            text.AppendLine("Lee esta tarea y ejecuta el trabajo solicitado. Termina con una respuesta final clara que resuma el resultado, los archivos cambiados, las pruebas y cualquier bloqueo. EasyMD capturara esa respuesta, la enviara automaticamente al equipo de origen y marcara la tarea como terminada; no accedas por tu cuenta al token ni a la cola EasyMD.");
            text.AppendLine();
            text.AppendLine("---");
            text.AppendLine();
            text.AppendLine(Value(task, "markdown"));
            File.WriteAllText(path, text.ToString(), Encoding.UTF8);
            return path;
        }

        private static void Log(string line)
        {
            Directory.CreateDirectory(Store.Dir);
            File.AppendAllText(Store.LogPath, "[" + DateTime.Now.ToString("s") + "] " + line + Environment.NewLine, Encoding.UTF8);
        }

        private static string SafeFileName(string name)
        {
            foreach (char c in Path.GetInvalidFileNameChars()) name = name.Replace(c, '-');
            name = name.Replace(' ', '-').Trim('-');
            return string.IsNullOrWhiteSpace(name) ? "handoff" : name;
        }

        private static string Value(Dictionary<string, object> dict, string key)
        {
            return dict.ContainsKey(key) && dict[key] != null ? Convert.ToString(dict[key]) : "";
        }
    }
}
