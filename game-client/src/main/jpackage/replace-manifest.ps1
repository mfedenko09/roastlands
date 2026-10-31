param([string]$Exe, [string]$Manifest)

$ErrorActionPreference = 'Stop'

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class ExeResources {
    [DllImport("kernel32", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern IntPtr BeginUpdateResource(string file, bool deleteExisting);

    [DllImport("kernel32", SetLastError = true)]
    public static extern bool UpdateResource(
        IntPtr handle, IntPtr type, IntPtr name, ushort language, byte[] data, uint size);

    [DllImport("kernel32", SetLastError = true)]
    public static extern bool EndUpdateResource(IntPtr handle, bool discard);
}
"@

$RT_MANIFEST = [IntPtr]24
$LAUNCHER_MANIFEST_ID = [IntPtr]1
$LANG_EN_US = 1033

Set-ItemProperty -Path $Exe -Name IsReadOnly -Value $false
$bytes = [IO.File]::ReadAllBytes($Manifest)
$handle = [ExeResources]::BeginUpdateResource($Exe, $false)
if ($handle -eq [IntPtr]::Zero) { throw "Cannot open $Exe for update" }
if (-not [ExeResources]::UpdateResource(
        $handle, $RT_MANIFEST, $LAUNCHER_MANIFEST_ID, $LANG_EN_US, $bytes, [uint32]$bytes.Length)) {
    throw "Cannot replace the manifest of $Exe"
}
if (-not [ExeResources]::EndUpdateResource($handle, $false)) { throw "Cannot save $Exe" }
