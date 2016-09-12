/*
 HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         =========== 
 |  10-July-2016     BhanuPrakash      R4    		  Initial Creatoin :  This calss perform submission operations once user clicks on submit button from Time Log view
*/
global class TimeLogSubmission {
	private static final String UNSUBMITTED = 'Unsubmitted';
	private static final String PENDING_SUBMITTED = 'Pending Submission';
		
    WebService static string SubmitRecords(String userId){
        //A String to hold the result of the operation
        string result = '';
        if (userId == null) {
            userId = userInfo.getUserId();
        }
        List<Time_Log__c>  timelogList = [select Id,Status__c, Send_to_Integration__c from Time_Log__c where OwnerId = :userId and (status__c =: UNSUBMITTED or (status__c =: PENDING_SUBMITTED AND LastModifiedDate <=:System.Now().addHours(-1)))   ];
		//List<Time_Log__c>  timelogList = [select Id,Status__c, Send_to_Integration__c from Time_Log__c where  (status__c = 'Unsubmitted' or status__c = 'Pending Submission') ];
       
		List<Time_Log__c> timeLogUpdatedList = new List<Time_Log__c>();
        if (timelogList.Size()!=0){
            For (Time_Log__c timeLogIterator : timeLogList) {
                if (timeLogIterator.status__c == Label.Time_Tracking_Unsubmitted) {
                    timeLogIterator.Status__c = Label.Time_Tracking_Pending_Submission;
                }
                timeLogIterator.Send_To_Integration__c = true;
                timeLogUpdatedList.add(timeLogIterator);
            }
        }
		Database.SaveResult[] srList = null;
        try {
            if (timelogList.Size()!=0){
                //update timeLogUpdatedList;
				srList = Database.Update(timeLogUpdatedList,false);
                result = label.Time_Tracking_Submission_Success + ' ' + String.Valueof(timeLogUpdatedList.Size());
            }
            else {
                result = label.Time_Tracking_Submission_No_Records;
            }
        }
        catch(DMLException dm) {
			// Iterate through each returned result
			/*for (Database.SaveResult sr : srList) {
				if (sr.isSuccess()) {
					// Operation was successful, so get the ID of the record that was processed
					System.debug('Successfully inserted account. Account ID: ' + sr.getId());
				}
				else {
					// Operation failed, so get all errors               
					for(Database.Error err : sr.getErrors()) {
						System.debug('The following error has occurred.');                   
						System.debug(err.getStatusCode() + ': ' + err.getMessage());
						System.debug('Account fields that affected this error: ' + err.getFields());
					}
				}
			}*/

            result = Label.Time_Tracking_Submission_Fail  + dm.getMessage();
        }
        return result;
    }
} 	