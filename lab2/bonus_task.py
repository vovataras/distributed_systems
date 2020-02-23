# Used to run commands
import subprocess
# Used to define OS
import platform


def select_command(usr_command):
    command = usr_command[0]
    args = usr_command[1:]

    def ping():
        subprocess.run(usr_command)
 
    def echo():
        if (platform.system() == 'Windows'):
            subprocess.run(['echo', ' '.join(args)], shell=True)
        else:
            subprocess.run(usr_command)
 
    def login():
        if (len(args) == 2):
            print(f'You are successfully logged in as "{args[0]}"')
            print(f'Password: {len(args[1])*"*"}')
        elif (len(args) == 1):
            print('You need to enter password!')
        else:
            print('You need to enter login and password!')
 
    def f_list():
        if (len(usr_command) > 1):
            print('Something went wrong')
        else:
            # Check the OS
            if (platform.system() == 'Windows'):
                subprocess.run("dir", shell=True)
            else:
                subprocess.run(["ls", "-la"])

    def msg():
        if (len(args) >= 2):
            msg = ' '.join(args[1:])
            print(f'You have successfully sent the message "{msg}" to "{args[0]}"')
        elif (len(args) == 1):
            print('You need to write message!')
        else:
            print('You need to enter destination user and message!')

    def f_file():
        if (len(args) > 1 and len(args) < 3):
            print(f'You have successfully sent the file "{args[1]}" to "{args[0]}"')
        elif (len(args) == 1):
            print('You need to enter filename!')
        elif (len(args) == 0):
            print('You need to enter destination user and filename!')
        else:
            print('You can send only one file!')

    def f_help():
        print("  ping  - test the ability of the source computer to reach a specified destination device;")
        print("  echo  - display line of text/string that are passed as an argument;")
        print("  login - establish a new session with the system (the command is fake);")
        print("  list  - list all files and subdirectories contained in directory;")
        print("  msg   - send a message to a specific user (the command is fake);")
        print("  file  - send a file to a specific user (the command is fake);")
        print("  exit  - close the command interpreter.")

    switcher = {
            "ping": ping,
            "echo": echo,
            "login": login,
            "list": f_list,
            "msg": msg,
            "file": f_file,
            "help": f_help
        }
 
    # Get the function from switcher dictionary
    func = switcher.get(command, "nothing")
    # Execute the function
    return func()


print('Use the "help" command to get help.')
while(True):
    user_input = input('[user@pc ]$ ' )
    if(len(user_input) == 0):
        print('Enter the command!')
        continue
    elif(user_input == 'exit'):
        exit()

    user_command = user_input.split()
    try:
        select_command(user_command)
    except:
        print('Wrong command')
