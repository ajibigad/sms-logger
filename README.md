# sms-logger
A simple android application that logs sms to a remote server.

It basically saves every text message(sms) recieved by the phone into a local sqlite db then sends them to a backend service whenever the phone is connected to the internet.

Its uses the Sync Framework in Android to sync the local db with a remote server so the messages are finally backed up on the remote server

### Things learnt:
1. Using a Broadcast reciever to listen to SMS recieved intents
2. Using Retrofit to interact with a backend service (Handles stuff like JSON serialization, HTTP etc)
3. Using SyncAdapter and Sqlite to keep messages synced between the phone and the backend service 
