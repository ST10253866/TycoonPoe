Introduction
======================
- This application Regalis is an offline and online card game app that makes use of game logic management, interactive user interfaces, and data tracking for rounds and scores. The application is built using Android Studio.

- It will allow the user to play a card game against a bot in the offline mode with rules such as revolution mode, pot clearing, and card trading between rounds.

- It also allow the user to play a card game against other real people online in a PVP mode with a total of 4 people with rules such as revolution mode, pot clearing, and card trading between rounds.

Installation steps(Desktop)
============================
- Ensure that you have Android studio installed on your laptop or desktop.

- You can then proceed to download the source code from the GitHub repository provided onto your machine.

- Once downloaded you can run the Android studio executable file in the project once in android studio 


Installation steps(on phone)
=============================
- Ensure that you download the APK file from the repository on to your phone and then install it 

- Once installed navigate to the app and open it and the app will open op to the login screen


 Features From Part 1 
======================
- The online game mode has a player messsaging feature that allows players to message each other once they are connected to the same room 

-  we allow player Visual customization in the form of them having the ability to change the card backs with the settings which will show for both the offline and online modes 

- The app has both offline and online mode which consists of the offline mode which is player vs bot and the online consists of 4 player vs player mode which is online and in real time 

Usage
======================
- (Desktop/laptop)To run this program you will need to do get the source code from the GitHub repository provided and run the program using Android studio that you have on your computer. From here you can run the program on Android studio and follow the prompts or steps that the program will show you.

- (Phone) To run this program you will need to do get the APK file from the GitHub repository provided and run the file to install app on your phone. From here navigate to
the app and open it.


How to use
======================
- The user will need to open the TycoonPOE app once downloaded. Once they have opened after downloading they will see the login page.

- If they are a new user, they will need to create an account to use the app by clicking the Register here button on the bottom of the login page. The user will have to enter their Username, email and set a password, then click the register button to register.Once Registered the user will then be moved to the home screen and can start using the app as normal 

- If they already have an account, they can simipler enter their email and password if they dont not want to login in with their email and password they can login using the Google sso at the bottom of the screen.Both the normal Login using thier email and passord and the use of the Google sso require the user to use thier phone biometrics in the form of a fingerprint before login to the app 

- Once logged in, the user will see a navigation bar at the bottom of the screen and be meet with the home screen this screen has three buttons on it which are PLay Game , Profile and logout as well as the bottom navigation bar which also has the same loction as the buttons 

- If the user click the play game button on the home screen or the Game mode button on the navigation bar it will take you to the Game mode screen it will show two main game mode buttons: Offline Mode and Online Mode.

- If the user selects the Offline mode it allows the user to play against a bot.The game / screen  will display the player’s hand and the bot’s moves.The user can play cards according to the rules, pass their turn, and attempt to clear the pot using special cards like 8s or four-of-a-kind.After rounds end, the game automatically manages the trading phase where players exchange cards depending on who lost the previous round

- If the user selects the online mode it allows the user to play against other players online. On that screen if the user presses the red menu button on the top left and the enter a room number once 4 player including themself enter the same room the game will start. The user will see their hand and can play cards in real time, with moves being synchronized with other players.The online game includes the same rules as offline mode, including pot clearing and revolution mode.This mode also as a messaging feature which always player to sends messages between each other and it displays the last message on the top of the player screen without blocking the game itself.Once Three round are played and overall win is decided 

- If the user selects the profile button or the profile icon on the navigation bar they will see a page that allows them to change there email and password while also having a setting button. 

- By the user select the settings button the user has the ability to select from three different card back which will show in both the online and offline game modes , ths user can also disable the biometrics for when login , they also have the ability to switch language to other which are not english but in part 2 it is still under development


Functional-Requirements
==========================
- The user will be able to register using their username, email, and password.

- The user must be able to log into the app using a password and email, or by using Google SSO.

- The user will be able to log in using their phone biometrics in the form of a fingerprint before accessing the app.

- The user will be able to select and switch between Offline Mode and Online Mode from the Game Mode screen.

- In Offline Mode, the user will be able to play against a bot. The system will enforce game rules, including special card rules like playing an 8 to clear the pot and four-of-a-kind triggering revolution mode.

- In Online Mode, the user will be able to join a room by entering a room number. The game supports real-time multiplayer for up to four players.

- The user will be able to send and receive messages in real time with other players while playing in online mode. The last message will always be displayed at the top of the player screen without blocking gameplay.

- The user will be able to view their hand at all times and play valid cards according to the rules, pass their turn, or clear the pot using special cards.

- The app will automatically handle the trading phase after each round, where the loser exchanges cards with the winner according to game rules.

- The user will be able to view and edit their profile, including changing their email and password.

- The user will be able to access a Settings page where they can change the card backs for both offline and online modes , enable or disable biometrics for login  , select a language other than English (feature under development).

- The user will have a navigation bar at the bottom of the screen with quick access to Home, Game Mode, Profile, and Logout.

- The app will provide a consistent and interactive interface for both offline and online gameplay, including smooth animations and clear visual feedback.

- The user will be able to log out from any screen using the logout button.



Non-Functional-Requirements
==============================
- The app will be compatible with Android devices running Android 7.0 or higher.

- The app will maintain real-time synchronization in Online Mode, ensuring smooth gameplay for up to 4 players.

-The app will handle network errors gracefully in Online Mode, notifying the user if connection is lost or a room cannot be joined.




Login details if needed 
=========================

- Email - bob@mail.com

- Password - 123456

GitHub Link For Api 
======================
https://github.com/ST10375204/tycoonTest


Youtube Link 
======================
https://youtu.be/TdMURH3Qta8



Credits
======================
Documentation( and comments in code) and validation

Karan Moodley – ST10256361

Contact: (Email) ST10256361@vcconnect.edu.za

Backend developer / api developer

Devesh Gokul – ST10375204

Contact: (Email) ST10375204@vcconnect.edu.za

Frontend developer / Graphic design 

Vivek Rajaram – ST10258124

Contact: (Email) ST10258124@vcconnect.edu.za

Backend and Frontend developer 

Aveshan Ryan Pillay - ST10253866

Contact: (Email) ST10253866@vcconnect.edu.za



References(Code attributions)
=================================
Andy’s Tech Tutorials (2023) GitHub Actions Tutorial | Run Automated Tests. https://www.youtube.com/watch?v=uFcXrWT4f80. (Accessed on 03 September  2025)

GeeksforGeeks (2025) Fragment Lifecycle in Android. https://www.geeksforgeeks.org/fragment-lifecycle-in-android/. (Accessed on 04 September 2025)

GeeksforGeeks (2025) Bottom Navigation Bar in Android. https://www.geeksforgeeks.org/bottom-navigation-bar-in-android/. (Accessed on 03 September 2025)

Foxandroid (2023) Bottom Navigation Bar - Android Studio | Fragments | Kotlin | 2023. https://www.youtube.com/watch?v=L_6poZGNXOo. (Accessed on 03 September 2025)

Server-Sent Events (SSE) in Android. https://medium.com/@pablo.nicolas.sabaliauskas/implementing-server-sent-events-sse-in-android-with-okhttp-eventsource-updated-764826f07e09 (Accessed on 03 September 2025)

Bitmap in Android .  https://www.geeksforgeeks.org/kotlin/download-image-from-url-in-android/ (Accessed on 04 September 2025)
