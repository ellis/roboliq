Set objShell = CreateObject("WScript.Shell")
' Set objExec = objShell.Exec("node C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js begin 100")
Return = objShell.Run("node C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js begin 100", 0, true)
