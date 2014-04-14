from SimpleXMLRPCServer import SimpleXMLRPCServer
from AutoItScript import *
import win32com.client
import time,os,shutil

UserName = "apiuser"
Password = "123456"

STATUS_LOADING = 2
STATUS_BUSY = 0x00020000
STATUS_RUNNING = 0x0100
STATUS_EXECUTIONERROR = 0x2000
STATUS_STOPPED = 0x00080000

evosys = 0
evodb = 0

evosystem = True

def IsEvo():
    if os.environ['COMPUTERNAME']=='MATH260-PC':
        return False
    else:
        return True
def __init__():
    # check which system are we on
    evosystem = IsEvo()
    if evosystem:
        # create the objects
        global evodb,evosys
        evodb = win32com.client.Dispatch("evoapi.database")
        evosys = win32com.client.Dispatch("evoapi.system")
        print "Objects inited"
def Logon():
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    # login with user and password
    print "Logging in using u/n: ",UserName," and pw: ",Password
    evosys.Logon(UserName,Password,0,1) # user / pass / evo / demo
    # evoware is slow, give it 1 second to get things straight
    time.sleep(1)
    WaitUntilReady()
    print "Log on successful, initialising"
    # initializing
    evosys.Initialize()
    WaitUntilReady()
    print "Initialise successful"

def IsReady(): # return if not busy loading or running a script
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    lstatus = evosys.GetStatus()
    #print lstatus
    return not ((lstatus & STATUS_LOADING) or ((lstatus >> 16) & (STATUS_BUSY >> 16)))
def WaitUntilReady():
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    # wait for loading/running to end
    while not IsReady():
        time.sleep(0.3)
def Logoff():
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    evosys.Logoff()
    print "Logging off"
    return 0
def PrepareScript(scriptname): # load a script in evoware
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    # remove recovery files to ignore previous failures
    os.system('del /F "C:\Program Files\TECAN\EVOware\database\scripts\*.rec"')
    scriptId = -1
    try:
        scriptId = evosys.PrepareScript(scriptname)
    except:
        print "failed to load the script, waiting 3 second and retrying"
        time.sleep(3)
        try:
            scriptId = evosys.PrepareScript(scriptname)
        except:
            print "Error loading script '",scriptname,"'"
    if scriptId is not -1:
        print "Successfully Loaded Script '",scriptname,"'"
    return scriptId
def ScriptResult(): # was the script that finished running successful?
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    lstatus = evosys.GetStatus()
    return not (((lstatus >> 16) & (STATUS_STOPPED >> 16)) > 0)
def LoadScript(scriptname):
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    Logon()
    id = PrepareScript(scriptname)
    Logoff()
def StartScript(scriptname,block=True): # if block then wait until script is finished
    if not evosystem:
        return RunAutoItScript('GeminiRunScript.au3',scriptname)

    result = True
    Logon()
    id = PrepareScript(scriptname)
    if id==-1:
        Logoff()
        return False
    # start the script
    print "Starting script, block=",block
    evosys.StartScript(id,0,0)
    time.sleep(1)
    if block:
        WaitUntilReady()
        result = ScriptResult()
        if not result:
            print "There was a problem running the script"
        Logoff()
    print "finished starting the script"
    return True
def OpenReader():
    print 'Opening door'
    RunAutoItScript('PlateReaderOpenDoor.au3')
def CloseReader():
    print 'Closing door'
    RunAutoItScript('PlateReaderCloseDoor.au3')
def MeasureReader(script,fname):
    print script,fname
    if os.path.exists('Measure.xls'):
        os.remove('Measure.xls')
    RunAutoItScript('PlateReaderRunScript.au3',script+' '+os.getcwd()+'\\Measure.xls')
    # make a copy of the output
    shutil.copy('Measure.xls',fname)
def Serve():
    if not evosystem:
        raise EnvironmentError(50,'Only runs in evo systems')
    # A simple server
    server = SimpleXMLRPCServer(("", 8000),logRequests=False)
    print "Listening on port 8000..."
    server.register_function(Logon)
    server.register_function(IsReady)
    server.register_function(WaitUntilReady)
    server.register_function(Logoff)
    server.register_function(PrepareScript)
    server.register_function(LoadScript)
    server.register_function(StartScript)
    server.register_function(ScriptResult)
    server.serve_forever()

__init__()


if __name__ == "__main__":
    print "Starting server"
    Serve()

