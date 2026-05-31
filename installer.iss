#define MyAppName "Materia Launcher"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Chokopieum Software"
#define MyAppExeName "materia-launcher.exe"
#define MyAppDirName "MateriaLauncher"

[Setup]
; AppId uniquely identifies this application.
; It uses the same upgradeUuid defined in build.gradle.kts
AppId={{019b375c-3319-7eec-8098-e50668c43b5a}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\{#MyAppDirName}
DisableProgramGroupPage=yes
LicenseFile=LICENSE
; Uncomment the following line to install for the current user only.
PrivilegesRequired=lowest
OutputDir=build\inno-setup
OutputBaseFilename=MateriaLauncher-Setup
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Путь к собранным файлам Compose Desktop (createDistributable)
Source: "build\compose\binaries\main\app\materia-launcher\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "build\compose\binaries\main\app\materia-launcher\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
