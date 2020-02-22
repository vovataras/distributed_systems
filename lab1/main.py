from datetime import datetime


def factorial(num):
    if (num == 1):
        return num
    elif (num <= 0):
        return 0

    return num * factorial(num - 1)


print('HELLo World!')
username = input('Please, enter your name: ')
len_username = len(username)

if (len_username == 0):
    print('You did not enter a name')
    exit()

print(f'Hello, {username}!')
print(f'Your name has {len_username} letters. {len_username}! = {factorial(len_username)}')


date_of_bd = input('Please, enter your birth date in format (DD.MM.YYYY): ')

try:
    date_of_bd = datetime.strptime(date_of_bd, '%d.%m.%Y')
except:
    print('You entered the wrong date')
    exit()


d = datetime.now()
difference_days = (d - date_of_bd).days
difference_years = int(difference_days // 365.2425)

if (difference_days < 0):
    print("You couldn't be born in the future)")
    exit()


print(f"Today is {d.strftime('%d.%m.%Y')}, you are "\
        + f'{difference_years} year{"s" if difference_years > 1 else ""} '\
        + f'({difference_days} day{"s" if difference_days > 1 else ""}) old.')
