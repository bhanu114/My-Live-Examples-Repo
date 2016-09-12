/*
 HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         =========== 
 |  04-JULY-2016       BhanuPrakash    R4    		Initial Creatoin
*/
trigger TimeLogsTrigger on Time_Log__c (after update) {
    
	if(Trigger.isAfter && Trigger.isUpdate){
	System.debug('**** in TimeLogsTrigger, berore calling performUpdateSRAccrud method' );
		TimeLogTirggerHelper.performUpdateSRAccrud(Trigger.oldMap,Trigger.newMap);
	System.debug('**** in TimeLogsTrigger, after calling performUpdateSRAccrud method' );
	}
}