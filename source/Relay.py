import win32com.client
import time

UserName = "apiuser"
Password = "123456"

STATUS_LOADING = 2
STATUS_BUSY = 0x00020000
STATUS_RUNNING = 0x0100
STATUS_EXECUTIONERROR = 0x2000
STATUS_STOPPED = 0x00080000

evosys = 0
evodb = 0

def __init__():
    # create the objects
    global evodb,evosys
    evodb = win32com.client.Dispatch("evoapi.database")
    evosys = win32com.client.Dispatch("evoapi.system")
    print("Objects inited")

def Logon():
    # login with user and password
    print("Logging in using u/n: ",UserName," and pw: ",Password)
    evosys.Logon(UserName, Password, 0, 0) # user / pass / evo / demo
    # evoware is slow, give it 1 second to get things straight
    time.sleep(1)
    WaitUntilReady()
    print("Log on successful, initialising")
    # initializing
    #evosys.Initialize()
    WaitUntilReady()
    print("Initialise successful")

def IsReady(): # return if not busy loading or running a script
    lstatus = evosys.GetStatus()
    #print lstatus
    return not ((lstatus & STATUS_LOADING) or ((lstatus >> 16) & (STATUS_BUSY >> 16)))

def WaitUntilReady():
    # wait for loading/running to end
    while not IsReady():
        time.sleep(0.3)

def Logoff():
    evosys.Logoff()
    print("Logging off")
    return 0

def PrepareScript(scriptname): # load a script in evoware
    # remove recovery files to ignore previous failures
    #os.system('del /F "C:\Program Files\TECAN\EVOware\database\scripts\*.rec"')
    scriptId = -1
    try:
        scriptId = evosys.PrepareScript(scriptname)
    except:
        print("failed to load the script, waiting 3 second and retrying")
        time.sleep(3)
        try:
            scriptId = evosys.PrepareScript(scriptname)
        except:
            print("Error loading script '",scriptname,"'")
    if scriptId is not -1:
        print("Successfully Loaded Script '",scriptname,"'")
    return scriptId

__init__()
Logon()
#PrepareScript("C:\\Program Files\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Light_1000.esc")
#evosys.Initialize()
