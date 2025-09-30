# task-4
SimpleRecommender.java   # Main application code
data/user_prefs.csv      # CSV file containing user-item ratings
UserID,ItemID,Rating
1,101,5
1,102,3
2,101,4
2,103,2
...
#compile to run
javac SimpleRecommender.java
#Run the program
java SimpleRecommender [TargetUserID] [TopN]
#Example
java SimpleRecommender 1 3
#Example output
Recommendations for User 1:
Item: 103 | Predicted score: 4.500
Item: 105 | Predicted score: 4.200
Item: 107 | Predicted score: 3.950


