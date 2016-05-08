' This script is used to avoid showing a visible terminal window when calling the CLI program from Evoware

Dim level

ReDim args(WScript.Arguments.Count-1)
For i = 0 To WScript.Arguments.Count-1
  args(i) = WScript.Arguments(i)
Next

'For Each arg In args
'  WScript.Echo arg
'Next

Set objShell = CreateObject("WScript.Shell")

If args(0) = "zlevel" Then
  level = Evoware.GetDoubleVariable("DETECTED_VOLUME_" & args(1))
  Return = objShell.Run("node C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js zlevel -- --well A01 --syringe 1 --zlevel " & level, 0, true)
Else
  Return = objShell.Run("node C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js -- " & Join(args, " "), 0, true)
End If
