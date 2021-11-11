from six.moves import input
from venmo_api import Client


# Get user's Venmo user ID from their username and password
# Automatically handles 2FA
def venmo_setup(username, password):
    # Get access token (they will have to do 2FA)
    access_token = Client.get_access_token(username=username, password=password)

    # Initialize client using access token
    client = Client(access_token=access_token)

    # Get user's Venmo ID, save to database
    client_profile = client.my_profile()
    user_id = client_profile.id

    # Print user ID
    print(f"user_id {user_id}")


# Request payments from other Venmo users from this Venmo account
# Given: requester's Venmo username and password, and string representations of lists of the following:
#   amounts to be requested, notes in each request, and Venmo user IDs for each requestee
def venmo_payments(username, password, amounts_str, notes_str, id_str):
    # Assumes lists are same size (checked in Kotlin)
    # Convert strings into lists
    amounts_arr = amounts_str.split("////")
    notes_arr = notes_str.split("////")
    id_arr = id_str.split("////")

    amounts_list = []
    notes_list = []
    id_list = []

    for i in range(0, len(amounts_arr)-1):
        amounts_list.append(amounts_arr[i])
        notes_list.append(notes_arr[i])
        id_list.append(id_arr[i])

    # Get access token (they will have to do 2FA)
    access_token = Client.get_access_token(username=username, password=password)

    # Initialize client using access token
    client = Client(access_token=access_token)

    # Initialize list of ids for unsuccessful payments
    unsuccessful_ids = []

    # Request payments for everyone in list
    for i in range(0, len(amounts_list)):
        success = client.payment.request_money(amount=float(amounts_list[i]), note=notes_list[i], target_user_id=id_list[i])
        if not success:
            unsuccessful_ids.append(id_list[i])

    # Print list of user IDs where requests were unsuccessful
    print(f"unsuccessful_ids {unsuccessful_ids}")
