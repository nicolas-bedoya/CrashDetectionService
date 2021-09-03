# CrashDetectionService
CrashDetection

## Basic purpose of activities:
The activities are in chronological order for sequence at which they are called upon within the application
### MainActivity 
Used to determine whether the set up of emergency contacts and user details have been completed
### ActivityPermissions 
Used to allow the user to accept permissions that are specific to the app
### AcitivityUserDetails 
Used to allow the user to input their details into the application 
### ActivityEmergencyContacts 
Used to add details of two emergency contacts 
### ActivityCrashDetection 
Used to start ride, where it will call a service and manage a crash event. In the event it will provide an alert dialog for clarification of crash, then send a text message to emergency contacts if no response
### ActivityService 
Calls sensors and retrieves data. If a suspicious event is registered from sensors then it will send necessary data to ActivityCrashDetection
### ActivityNotification
Used to create notification channel for notification of foreground service, and notification of potential crash detected

