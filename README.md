___________________________________________________________________________________________________________
FORMULA TO RERURN BOOLEAN IF DIFF. B/W START AND END DATE IS 14 BUSINESS DAYS

if(CASE(MOD( ( bhanuSpace__Test_Start_Date__c )  - DATE(1985,6,24),7), 

0 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ) ,7),1,2,2,3,3,4,4,5,5,5,6,5,1), 
1 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,2,2,3,3,4,4,4,5,4,6,5,1), 
2 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,2,2,3,3,3,4,3,5,4,6,5,1), 
3 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,2,2,2,3,2,4,3,5,4,6,5,1), 
4 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,1,2,1,3,2,4,3,5,4,6,5,1), 
5 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,0,2,1,3,2,4,3,5,4,6,5,0), 
6 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,1,2,2,3,3,4,4,5,5,6,5,0), 
999) 
+ 
(FLOOR(( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ))/7)*5) == 14,true,false)
_______________________________________________________________________________________________________________

FORMULA TO RERURN BOOLEAN IF DATE IS BEFORE 14 BUSINESS DAYS - STARTING FROM TODAY

if(CASE(MOD( ( TODAY()  )  - DATE(1985,6,24),7), 

0 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ) ,7),1,2,2,3,3,4,4,5,5,5,6,5,1), 
1 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,2,2,3,3,4,4,4,5,4,6,5,1), 
2 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,2,2,3,3,3,4,3,5,4,6,5,1), 
3 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,2,2,2,3,2,4,3,5,4,6,5,1), 
4 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,1,2,1,3,2,4,3,5,4,6,5,1), 
5 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,0,2,1,3,2,4,3,5,4,6,5,0), 
6 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,1,2,2,3,3,4,4,5,5,6,5,0), 
999) 
+ 
(FLOOR(( bhanuSpace__Test_End_Date__c - ( TODAY() ))/7)*5) == 14,true,false)
_______________________________________________________________________________________________________________

FORMULA TO RERURN BOOLEAN IF DATE IS BEFORE 14 BUSINESS DAYS

if(CASE(MOD( ( bhanuSpace__Test_End_Date__c -14 )  - DATE(1985,6,24),7), 

0 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,2,2,3,3,4,4,5,5,5,6,5,1), 
1 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,2,2,3,3,4,4,4,5,4,6,5,1), 
2 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,2,2,3,3,3,4,3,5,4,6,5,1), 
3 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,2,2,2,3,2,4,3,5,4,6,5,1), 
4 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,1,2,1,3,2,4,3,5,4,6,5,1), 
5 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,0,2,1,3,2,4,3,5,4,6,5,0), 
6 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_End_Date__c -14 ),7),1,1,2,2,3,3,4,4,5,5,6,5,0), 
999) 
+ 
(FLOOR(( bhanuSpace__Test_End_Date__c - ( bhanuSpace__Test_End_Date__c -14 ))/7)*5) == 14,true,false)
_______________________________________________________________________________________________________________


*****************************************************************************************************************
FIDND DAYS B/W START AND ENDDATES DIFF:
CASE(MOD( ( bhanuSpace__Test_Start_Date__c )  - DATE(1985,6,24),7), 

0 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c) ,7),1,2,2,3,3,4,4,5,5,5,6,5,1), 
1 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,2,2,3,3,4,4,4,5,4,6,5,1), 
2 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,2,2,3,3,3,4,3,5,4,6,5,1), 
3 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,2,2,2,3,2,4,3,5,4,6,5,1), 
4 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,1,2,1,3,2,4,3,5,4,6,5,1), 
5 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,0,2,1,3,2,4,3,5,4,6,5,0), 
6 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ),7),1,1,2,2,3,3,4,4,5,5,6,5,0), 
999) 
+ 
(FLOOR(( bhanuSpace__Test_End_Date__c- ( bhanuSpace__Test_Start_Date__c ))/7)*5)
______________________________________________________________________________

FIDND DAYS DIFF. TODAYS DATE AND FUTURE DATE :

CASE(MOD( ( TODAY() )  - DATE(1985,6,24),7), 

0 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY()) ,7),1,2,2,3,3,4,4,5,5,5,6,5,1), 
1 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,2,2,3,3,4,4,4,5,4,6,5,1), 
2 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,2,2,3,3,3,4,3,5,4,6,5,1), 
3 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,2,2,2,3,2,4,3,5,4,6,5,1), 
4 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,1,2,1,3,2,4,3,5,4,6,5,1), 
5 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,0,2,1,3,2,4,3,5,4,6,5,0), 
6 , CASE( MOD( bhanuSpace__Test_End_Date__c- ( TODAY() ),7),1,1,2,2,3,3,4,4,5,5,6,5,0), 
999) 
+ 
(FLOOR(( bhanuSpace__Test_End_Date__c- ( TODAY() ))/7)*5)

**********************************************************************************************

FIND 1 YEAR LATER DATE

IF(DATE( YEAR( TODAY()) , MONTH( TODAY()), DAY( TODAY()) ) >= DATE( YEAR(bhanuSpace__Test_End_Date__c) +1 , MONTH( bhanuSpace__Test_End_Date__c), DAY( bhanuSpace__Test_End_Date__c) ),TRUE,FALSE)
