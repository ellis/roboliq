' This script is used to avoid showing a visible terminal window when calling the CLI program from Evoware

Dim level, jspath, cmd

ReDim args(WScript.Arguments.Count-1)
For i = 0 To WScript.Arguments.Count-1
  args(i) = WScript.Arguments(i)
Next

'For Each arg In args
'  WScript.Echo arg
'Next

Set objShell = CreateObject("WScript.Shell")

'If args(0) = "zlevel" Then
'  level = Evoware.GetDoubleVariable("DETECTED_VOLUME_" & args(1))
'  Return = objShell.Run("node C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js zlevel -- --well A01 --syringe 1 --zlevel " & level, 0, true)
'Else

'Wscript.Echo "Script path: " & Wscript.ScriptFullName
jspath = Left(WScript.ScriptFullName, Len(WScript.ScriptFullName) - 3)
cmd = "node " & jspath & "js " & Join(args, " ")
'WScript.Echo cmd
Return = objShell.Run(cmd, 0, true)
WScript.Quit Return
