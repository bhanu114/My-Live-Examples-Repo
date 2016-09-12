/*
 HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         =========== 
 |  02-July-2016       BhanuPrakash    R4    		Initial Creatoin
*/

public class TimeLogTirggerHelper {
	private static final String SUBMITTED = 'Submitted';
	private static final String UNSUBMITTED = 'Unsubmitted';
	private static final String PENDINGSUBMISSION = 'Pending Submission';
	public static List<Case> caseList = new List<Case>();
	public static List<Time_Log__c> tlList = new List<Time_Log__c>();
	
	public static void performUpdateSRAccrud(Map<Id, Time_Log__c> tlOldMap, Map<Id, Time_Log__c> tlNewMap){
		//GET ALL SRs TO PERFORM
		Set<String> srNoSet = new Set<String>();
		Set<String> srIdSet = new Set<String>();
		Set<String> tlIdSet = new Set<String>();
		for(Time_Log__c tl : tlNewMap.values()){
			if(tl.Status__c.equals(SUBMITTED) && (tlOldMap.get(tl.Id).Status__c.equals(UNSUBMITTED) || tlOldMap.get(tl.Id).Status__c.equals(PENDINGSUBMISSION))){
				if(tl.Service_Request__c != null){
					srIdSet.add(tl.Service_Request__c);
				} else if(tl.Unauthorized_Service_Request__c != null && tl.Unauthorized_Service_Request__c != ''){
					srNoSet.add(tl.Unauthorized_Service_Request__c);
				}
				tlIdSet.add(tl.Id);
			}
		}
		System.debug('**** srNoSet = ' + srNoSet + ' :  srIdSet = ' + srIdSet);
		if(!srIdSet.isEmpty() || !srNoSet.isEmpty()){
			caseList = [select caseNumber,Service_Request_Age__c,Status from case where CaseNumber IN : srNoSet OR Id IN : srIdSet Limit 50000] ;
		}
		System.debug('**** caseList = ' + caseList);
		//GET ALL TT RECORDS RELATED TO ALL SR's
		if(!caseList.isEmpty()){
			String query = 'SELECT Id, Accrued_Actual_Time__c,Unauthorized_Service_Request__c,Service_Request__c, Service_Request__r.caseNumber FROM Time_Log__c WHERE (ID IN :tlIdSet OR Status__c =:SUBMITTED) AND ';
			if(!srNoSet.isEmpty()){
				query += ' Unauthorized_Service_Request__c IN: srNoSet ';
			}
			if(!srIdSet.isEmpty()){
				if(!srNoSet.isEmpty()){
					query += ' OR ';
				}
				query += ' Service_Request__c IN: srIdSet ';
			}
			System.debug('*** query = ' + query);
			
			tlList = Database.query(query);
			
			System.debug('**** tlList = ' + tlList);
			//PREPARE MAP OF SR's AND ACCRUD VALUE TO BE UPDATED FROM TT ACCRUDS
			Map<String, Decimal> srNoAccrudValMap = new Map<String, Decimal>();
			String srNumber = '';
			Decimal accrVal = 0;
			for(Time_Log__c tl : tlList){
				srNumber = (tl.Service_Request__c != null)? tl.Service_Request__r.caseNumber : tl.Unauthorized_Service_Request__c;
				accrVal = srNoAccrudValMap.get(srNumber);
				if(accrVal == null){
					accrVal = 0;
				}
				accrVal += tl.Accrued_Actual_Time__c;
				srNoAccrudValMap.put(srNumber, accrVal);
			}
			System.debug('**** srNoAccrudValMap = ' + srNoAccrudValMap);
			
			Map<String, Case> srNoSRMap = new Map<String, Case>();
			
			if(!srNoAccrudValMap.isEmpty()){
				for(Case c : caseList){
					if(srNoAccrudValMap.get(c.caseNumber) != null){
						c.Submitted_Accrued_Time_Number__c = srNoAccrudValMap.get(c.caseNumber);
						srNoSRMap.put(c.caseNumber, c);
					}
				}
			}
			System.debug('**** befoer updating SR ');
			Database.Update(srNoSRMap.values());
			System.debug('**** after updating SR ');
		}//perform on case not empty - End
		
	}
}