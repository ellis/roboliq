ReDim args(WScript.Arguments.Count-1)
For i = 0 To WScript.Arguments.Count-1
  args(i) = WScript.Arguments(i)
Next

'For Each arg In args
'  WScript.Echo arg
'Next

Set objShell = CreateObject("WScript.Shell")
'Return = objShell.Run("node C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js begin 100", 0, true)
'Return = objShell.Run("node C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js begin 100", 0, true)
Return = objShell.Run("node C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js " & Join(args, " "), 0, true)



'CSCRIPT MyScript.vbs

'WScript.Arguments.Item(0)
'WScript.Arguments.Item(1)
