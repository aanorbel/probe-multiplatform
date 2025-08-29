!define APP_NAME "OONI Probe"
!define APP_VERSION "${android.defaultConfig.versionName}"
!define APP_ID "${config.appId}"

!include "MUI2.nsh"

; MUI Setup
!define MUI_ABORTWARNING
!define MUI_ICON "${projectDir}/icons/app.ico" ; Assuming you have an icon file
!define MUI_UNICON "${projectDir}/icons/app.ico"

; Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${projectDir}/LICENSE" ; Assuming you have a LICENSE file
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

; Uninstall pages
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; Language
!insertmacro MUI_LANGUAGE "English"

; Custom Actions for Protocol Registration
Section "URL Protocol Registration" SEC_URL_PROTOCOL
    ; Register OONI protocol handler
    WriteRegStr HKLM "Software\Classes\ooni" "" "URL:OONI Probe Protocol"
    WriteRegStr HKLM "Software\Classes\ooni" "URL Protocol" ""
    WriteRegStr HKLM "Software\Classes\ooni\DefaultIcon" "" '"$INSTDIR\${APP_NAME}.exe",0'
    WriteRegStr HKLM "Software\Classes\ooni\shell\open\command" "" '"$INSTDIR\${APP_NAME}.exe" "%1"'

    ; Add to PATH environment variable (optional, but good for command-line access)
    ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\${APP_NAME}.exe"
    StrCmp $R0 "" 0 +2
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\${APP_NAME}.exe" "" "$INSTDIR\${APP_NAME}.exe"

SectionEnd

; Main section
Section "Main Application" SEC_MAIN
    SetOutPath "$INSTDIR"

    ; Add files to the installer
    File /r "${projectDir}\composeApp\build\install\${APP_NAME}\*"

    ; Write the uninstall information
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "DisplayName" "${APP_NAME}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "UninstallString" '"$INSTDIR\Uninstall.exe" /S'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "NoRepair" 1
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "Publisher" "${organization}" ; Assuming 'organization' is available from project properties
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "DisplayVersion" "${APP_VERSION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "DisplayIcon" '"$INSTDIR\${APP_NAME}.exe"'

    ; Create a shortcut for the application
    CreateShortCut "$DESKTOP\${APP_NAME}.lnk" "$INSTDIR\${APP_NAME}.exe" "" "$INSTDIR\${APP_NAME}.exe" 0

SectionEnd

; Uninstall section
Section "Uninstall"
    ; Remove the application files
    Delete "$INSTDIR\Uninstall.exe"
    Delete "$INSTDIR\${APP_NAME}.exe"
    ; ... delete other files and directories installed by the app ...

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
    DeleteRegKey HKLM "Software\Classes\ooni"

    ; Remove shortcut
    Delete "$DESKTOP\${APP_NAME}.lnk"

    ; Remove directory
    RmDir /r "$INSTDIR"

SectionEnd
