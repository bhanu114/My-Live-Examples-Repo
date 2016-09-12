/*
 HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         =========== 
 |  23-MAY-2016       BhanuPrakash    R4            Initial Creatoin
*/
/*
*   Class Name  :   SpecialInstructionController
*   Class Type  :   Controller
*/
public class TimeTrackingController {

    public string recordId {get;set;}
    public Time_Log__c tTracking {get;set;}{tTracking = new Time_Log__c();}
    public List<Time_Log__c> oldTLogList = new  List<Time_Log__c>();
    public List<Time_Log__c> unsubTLogList = new  List<Time_Log__c>();
    public String autoORManualSRRQNumber {set; get;}
	public Case curentSR {set; get;} {curentSR = new Case();}
    public List<Case> caseList {set; get;}{caseList = new List<Case>();}
	public Decimal totUnsubAcrudTime = 0;
	public Integer totUnsubAcrudHrs {set; get;}
	public Integer totUnsubAcrudMins{set; get;}
    
    
    public TimeTrackingController(ApexPages.StandardController controller) {
		
		init();
    }
	
    public  void init(){
		//Your time logged has exceeded 16 hours. Please submit your time.
		//BusinessHours bh = [select Id from BusinessHours where IsDefault=true];
		//DateTime brfor16Hours =	BusinessHours.add(bh.Id, Datetime.Now (), -57600000);
        //SOQL on Case with record ID
		totUnsubAcrudTime = 0;
        unsubTLogList = [SELECT Id, Accrued_Actual_Time__c, CreatedDate FROM Time_Log__c WHERE Status__c = 'Unsubmitted' AND createdbyID =: UserInfo.getUserId() AND Status__c != 'Submitted' AND Status__c != 'Pending Submission' Limit 1000];
		for(Time_Log__c tl : unsubTLogList){
			totUnsubAcrudTime += tl.Accrued_Actual_Time__c;
		}
		System.debug('*** totUnsubAcrudTime = ' + totUnsubAcrudTime);
		totUnsubAcrudHrs = Integer.valueOf(Math.floor(totUnsubAcrudTime/60));
		totUnsubAcrudMins = Math.mod(Integer.valueOf(totUnsubAcrudTime),60);//Integer.valueOf(totUnsubAcrudTime/60);
		unsubTLogList = [SELECT Id, Accrued_Actual_Time__c, CreatedDate FROM Time_Log__c WHERE Status__c = 'Unsubmitted' AND createdbyID =: UserInfo.getUserId() AND Status__c != 'Submitted'  Limit 1000];
    }
    /*
    *   Method Name         :   SpecialInstructionController
    *   Method Type         :   Constructor
    *   Method Visibility   :   Public
    *   Use Of this Method :    To Initialize variables to show in VF page
    *   Parameter List      :   NO
    *   Return Type         :   NA
    */
    public TimeTrackingController(){
       String caseId = System.currentPageReference().getParameters().get('Id').trim();
        System.debug('**** exten, caseId = ' + caseId); 
    }
    /*
    *   Method Name         :   search
    *   Method Type         :   Instance
    *   Method Visibility   :   Public
    *   Use Of this Method :    To perform search based on user selected Account record type and search string
    *   Parameter List      :   NO
    *   Return Type         :   PageReference
    */
    
    public PageReference submit(){
		Set<String> exceptionalSRRTIDs = new Set<String>( TT_ExceptSRRecordTypeIDs__c.getInstance().ExceptRTIDs__c.split(','));
		
		String selectedMode =  ApexPages.currentPage().getParameters().get('selection');
		selectedMode = selectedMode.trim();
		String unAuthorizeSrNo =  ApexPages.currentPage().getParameters().get('unAuthorizeSrNo');
		unAuthorizeSrNo = unAuthorizeSrNo.trim();
		
		if(selectedMode != null && selectedMode.equals('autharized')){
			System.debug('**** You selected : ' + selectedMode);
		} else if(selectedMode != null && selectedMode.equals('unautharized')){
			if(unAuthorizeSrNo != null && !unAuthorizeSrNo.equals('')){
				//Auto fill 0's
				String auto0s = '';
				if(unAuthorizeSrNo.length() <= 8){
					for(Integer i=unAuthorizeSrNo.length(); i < 8;i++){
						auto0s += '0';
					}
				}
				unAuthorizeSrNo = auto0s+unAuthorizeSrNo;
				autoORManualSRRQNumber = unAuthorizeSrNo;
				caseList = [select caseNumber,Service_Request_Age__c,Status,RecordTypeId,ClosedDate from case where CaseNumber = : autoORManualSRRQNumber Limit 1] ;
			}
		}
        System.debug('*** submit, autoORManualSRRQNumber = ' + autoORManualSRRQNumber);
		
		if(autoORManualSRRQNumber == null || autoORManualSRRQNumber.equals('')){
            ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.TT_FillSRNumber));
            return returnNULL();
        
        }
		
		if(caseList.isEmpty()){
			ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.SI_EnterValiedSRNumber));
			return returnNULL();	
		} else if( exceptionalSRRTIDs.contains(((String)caseList[0].RecordTypeId).substring(0,15)) ){
			ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.TT_CooseOtherRT));
			return returnNULL();
		} else if( caseList[0].Status.equals('Closed')) {//Time cannot be logged on an SR that has a closed date more of than 1 week. Req. 151091
			Long cCloseTime = caseList[0].ClosedDate.getTime();
			Long presentTime = DateTime.now().getTime();
			Long milliseconds = presentTime - cCloseTime;
			Long days2 = (((milliseconds / 1000)/60)/60)/24;
			if(days2 > 7){
				ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.TT_CantEnterTTIfCloseDateMoreThan7));
				return returnNULL();
			}
		}
		
		if(!isUpseartable(tTracking)){
			 return returnNULL();
		} else {
            String userId = UserInfo.getUserId();
            String srId = autoORManualSRRQNumber;
			String dateMe = String.valueOf(System.today());
			
            String uniqueId = srId + '-' + userId + '-' + dateMe ;
            
            //CHECK IF THIS SHOULD BE CREATE OF UPDATE EVENT BASED ON UNIQUE ID
            oldTLogList = [SELECT Id, Accrued_Actual_Time__c, Case_Time__c,Exception_Handling__c,Knowledge__c,Resource_Management__c,SR_Create__c,SR_Admin__c,Unique_Identifier__c,Service_Request__c,Status__c FROM Time_Log__c WHERE Unique_Identifier__c =: uniqueId AND Status__c = 'Unsubmitted' Limit 1];
            //try{
				//if(oldTLogList.size() > 0 && !oldTLogList[0].Status__c.equals('Pending Submission') && !oldTLogList[0].Status__c.equals('Submitted') && oldTLogList[0].Status__c.equals('Unsubmitted')){//update existing TT record
				if(oldTLogList.size() > 0 ){	
				System.debug('*** in update tTracking = ' + tTracking);
				//Check time against an SR greater than how long it has been open
				if( isAgeIsLess(oldTLogList[0],true)){
					System.debug('*** Actual time is more than SR AGE' );
					ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.TT_SRAgeIsLess));
					return returnNULL();
				}
					//tTracking.Accrued_Actual_Time__c = tTracking.Accrued_Actual_Time__c + oldTLog.Accrued_Actual_Time__c;
				
					if (Schema.sObjectType.Time_Log__c.fields.Case_Time__c.isUpdateable() && tTracking.Case_Time__c != null && String.valueOf(tTracking.Case_Time__c) != '') {
						
						System.debug('*** before setting  oldTLogList[0].Case_Time__c = ' + oldTLogList[0].Case_Time__c + ' : tTracking.Case_Time__c = ' + tTracking.Case_Time__c);
						oldTLogList[0].Case_Time__c = tTracking.Case_Time__c + ((oldTLogList[0].Case_Time__c != null) ? oldTLogList[0].Case_Time__c : 0);
					}
					if (Schema.sObjectType.Time_Log__c.fields.Exception_Handling__c.isUpdateable() && tTracking.Exception_Handling__c != null && String.valueOf(tTracking.Exception_Handling__c) != '') {
						System.debug('*** before setting  oldTLogList[0].Exception_Handling__c = ' + oldTLogList[0].Exception_Handling__c  + ' : tTracking.Exception_Handling__c = ' + tTracking.Exception_Handling__c);
						oldTLogList[0].Exception_Handling__c = tTracking.Exception_Handling__c + ((oldTLogList[0].Exception_Handling__c != null) ? oldTLogList[0].Exception_Handling__c : 0);
					}
					if (Schema.sObjectType.Time_Log__c.fields.Knowledge__c.isUpdateable() && tTracking.Knowledge__c != null && String.valueOf(tTracking.Knowledge__c) != '') {
						System.debug('*** before setting  oldTLogList[0].Knowledge__c = ' + oldTLogList[0].Knowledge__c + ' : tTracking.Knowledge__c = ' + tTracking.Knowledge__c);
						oldTLogList[0].Knowledge__c = tTracking.Knowledge__c + ((oldTLogList[0].Knowledge__c != null) ? oldTLogList[0].Knowledge__c : 0);
					}
					if (Schema.sObjectType.Time_Log__c.fields.SR_Create__c.isUpdateable() && tTracking.SR_Create__c != null && String.valueOf(tTracking.SR_Create__c) != '') {
						System.debug('*** before setting  oldTLogList[0].SR_Create__c = ' + oldTLogList[0].SR_Create__c  + ' : tTracking.SR_Create__c = ' + tTracking.SR_Create__c);
						oldTLogList[0].SR_Create__c = tTracking.SR_Create__c + ((oldTLogList[0].SR_Create__c != null) ? oldTLogList[0].SR_Create__c : 0);
					}
					if (Schema.sObjectType.Time_Log__c.fields.Resource_Management__c.isUpdateable() && tTracking.Resource_Management__c != null && String.valueOf(tTracking.Resource_Management__c) != '') {
						System.debug('*** before setting  oldTLogList[0].Resource_Management__c = ' + oldTLogList[0].Resource_Management__c  + ' : tTracking.Resource_Management__c = ' + tTracking.Resource_Management__c);
						oldTLogList[0].Resource_Management__c = tTracking.Resource_Management__c + ((oldTLogList[0].Resource_Management__c != null) ? oldTLogList[0].Resource_Management__c : 0);
					}
					if (Schema.sObjectType.Time_Log__c.fields.SR_Admin__c.isUpdateable() && tTracking.SR_Admin__c != null && String.valueOf(tTracking.SR_Admin__c) != '') {
						System.debug('*** before setting  oldTLogList[0].SR_Admin__c = ' + oldTLogList[0].SR_Admin__c);
						oldTLogList[0].SR_Admin__c = tTracking.SR_Admin__c + ((oldTLogList[0].SR_Admin__c != null) ? oldTLogList[0].SR_Admin__c : 0);
					}
					Database.Update(oldTLogList);
					//Refresh Unsubmited time
					init();
					 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.INFO,Label.TT_RecordUpdated));
					
					System.debug('*** Updated TT record *******');
				} else {//Insert new TT record
					System.debug('*** Perform Insert *******');
					if(!caseList.isEmpty()){
						if(selectedMode != null && selectedMode.equals('autharized')){
							tTracking.Service_Request__c = caseList[0].Id;
						} else if(selectedMode != null && selectedMode.equals('unautharized')){
								tTracking.Unauthorized_Service_Request__c = autoORManualSRRQNumber;
								tTracking.Service_Request__c = caseList[0].Id;
						}
					}
					tTracking.Unique_Identifier__c = uniqueId;
					
					//Check time against an SR greater than how long it has been open
					if( isAgeIsLess(tTracking,false)){
						ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.TT_SRAgeIsLess));
						return returnNULL();
					}
					
					try{
						Database.Insert(tTracking);
						//Refresh Unsubmited time
						init();
						 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.INFO,Label.TT_NewRecordCreated));
					}catch(Exception ex){
						if(ex.getMessage().contains(Label.TT_ValidationSRMorethanAWeekAge)){
							ApexPages.addmessage(new ApexPages.message(ApexPages.severity.INFO,Label.TT_ValidationSRMorethanAWeekAge));
							returnNULL();
						}else if(ex.getMessage().contains(Label.TT_ValidationEnterAtLeast1SRNo)){
							ApexPages.addmessage(new ApexPages.message(ApexPages.severity.INFO,Label.TT_ValidationEnterAtLeast1SRNo));
							returnNULL();
						}
					}
				}
        }
		
		return returnNULL();
    }
	//Req. #145340
	private Boolean isAgeIsLess(Time_Log__c tempTTracking, Boolean isUpdate){
		List<String> ageList = (List<String>) caseList[0].Service_Request_Age__c.remove(',').split(' ');
		//CHECK IF REQUEST AGE IS EMPTY
		if(ageList.size() < 6){
			return true;
		}
		
		Integer days = Integer.valueOf(ageList[0]);
		Integer hours = Integer.valueOf(ageList[2]);
		Integer mins = Integer.valueOf(ageList[4]);
		Integer srAgetotMins = (days * 24 * 60);
		srAgetotMins += (hours * 60);
		srAgetotMins += mins;
		
		Integer tempAccrudTime = 0; //Integer.valueOf(tTracking.Case_Time__c + tTracking.Exception_Handling__c + tTracking.Knowledge__c + tTracking.Resource_Management__c + tTracking.SR_Create__c + tTracking.SR_Admin__c);
		if(isUpdate){
			System.debug('**** isUpdate = ' + isUpdate);
			if (Schema.sObjectType.Time_Log__c.fields.Case_Time__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(tempTTracking.Case_Time__c + this.tTracking.Case_Time__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.Exception_Handling__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(tempTTracking.Exception_Handling__c  + this.tTracking.Exception_Handling__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.Knowledge__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(tempTTracking.Knowledge__c  + this.tTracking.Knowledge__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.Resource_Management__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(tempTTracking.Resource_Management__c  + this.tTracking.Resource_Management__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.SR_Create__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(tempTTracking.SR_Create__c  + this.tTracking.SR_Create__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.SR_Admin__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(tempTTracking.SR_Admin__c  + this.tTracking.SR_Admin__c);
			}
		} else {//On TT update
			System.debug('**** isUpdate = ' + isUpdate);
			if (Schema.sObjectType.Time_Log__c.fields.Case_Time__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(this.tTracking.Case_Time__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.Exception_Handling__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf( this.tTracking.Exception_Handling__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.Knowledge__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(this.tTracking.Knowledge__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.Resource_Management__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(this.tTracking.Resource_Management__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.SR_Create__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf( this.tTracking.SR_Create__c);
			}
			if (Schema.sObjectType.Time_Log__c.fields.SR_Admin__c.isUpdateable()) {
				tempAccrudTime += Integer.valueOf(this.tTracking.SR_Admin__c);
			}
		}
		System.debug('*** tempAccrudTime = ' + tempAccrudTime);
		
		if(srAgetotMins < Integer.valueOf(tempAccrudTime)){
			return true;
		}
		return false;
	}
	//Calling from TimeTrackingPage page javascrip
    public void getPrimaryTabdata(){
        if(Apexpages.currentPage().getParameters().get('recordId') != null){
            recordId= Apexpages.currentPage().getParameters().get('recordId');   
        }
        system.debug('**** recordId '+recordId);
		//Check if the id it Case Id
		if(recordId != null && recordId.startsWith('500')){
			if(recordId != null && recordId != 'null'){
				caseList = [select caseNumber,Service_Request_Age__c,Status,RecordTypeId,ClosedDate from case where id = :recordId Limit 1] ;
				autoORManualSRRQNumber = caseList[0].caseNumber;
			}
		} else {
			autoORManualSRRQNumber = '';
		}
		//Refresh the unsumitted hours calculations
		init();
    }
	
	public Boolean isUpseartable(Time_Log__c tl){
		System.debug('*** in, isUpseartable' );
		if((Schema.sObjectType.Time_Log__c.fields.Case_Time__c.isUpdateable() && tl.Case_Time__c != null && tl.Case_Time__c > 0 )
			|| (Schema.sObjectType.Time_Log__c.fields.Exception_Handling__c.isUpdateable() && tl.Exception_Handling__c != null && tl.Exception_Handling__c > 0 )
			|| (Schema.sObjectType.Time_Log__c.fields.Knowledge__c.isUpdateable() && tl.Knowledge__c != null && tl.Knowledge__c > 0 )
			|| (Schema.sObjectType.Time_Log__c.fields.Resource_Management__c.isUpdateable() && tl.Resource_Management__c != null && tl.Resource_Management__c > 0 )
			|| (Schema.sObjectType.Time_Log__c.fields.SR_Create__c.isUpdateable() && tl.SR_Create__c != null && tl.SR_Create__c > 0 )
			|| (Schema.sObjectType.Time_Log__c.fields.SR_Admin__c.isUpdateable() && tl.SR_Admin__c != null && tl.SR_Admin__c > 0 ) ) {
				
			//Please enter a number less than 3 digits per entry.  Req. 146157
			if((Schema.sObjectType.Time_Log__c.fields.Case_Time__c.isUpdateable() && (String.valueOf(tl.Case_Time__c).length() > 3 ))
				|| (Schema.sObjectType.Time_Log__c.fields.Exception_Handling__c.isUpdateable() && (String.valueOf(tl.Exception_Handling__c).length() > 3))
				|| (Schema.sObjectType.Time_Log__c.fields.Knowledge__c.isUpdateable() &&  (String.valueOf(tl.Knowledge__c).length() > 3 ))
				|| (Schema.sObjectType.Time_Log__c.fields.Resource_Management__c.isUpdateable() && (String.valueOf(tl.Resource_Management__c).length() > 3 ))
				|| (Schema.sObjectType.Time_Log__c.fields.SR_Create__c.isUpdateable() && (String.valueOf(tl.SR_Create__c).length() > 3 ))
				|| (Schema.sObjectType.Time_Log__c.fields.SR_Admin__c.isUpdateable() &&  (String.valueOf(tl.SR_Admin__c).length() > 3 ) )) {
				System.debug('***  Please enter a number less than 4 digits per entry ' );
				ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,'Please enter a number less than 4 digits per entry'));
				return false;
			} else {
				return true;
			} 
			
			//VALIDATION : Entered value should not be empty
			if((Schema.sObjectType.Time_Log__c.fields.Case_Time__c.isUpdateable() && (String.valueOf(tTracking.Case_Time__c) == '' ))
				|| (Schema.sObjectType.Time_Log__c.fields.Exception_Handling__c.isUpdateable() && (String.valueOf(tTracking.Exception_Handling__c) == ''))
				|| (Schema.sObjectType.Time_Log__c.fields.Knowledge__c.isUpdateable() &&  (String.valueOf(tTracking.Knowledge__c) == '' ))
				|| (Schema.sObjectType.Time_Log__c.fields.Resource_Management__c.isUpdateable() && (String.valueOf(tTracking.Resource_Management__c) == ''))
				|| (Schema.sObjectType.Time_Log__c.fields.SR_Create__c.isUpdateable() && (String.valueOf(tTracking.SR_Create__c) == '' ))
				|| (Schema.sObjectType.Time_Log__c.fields.SR_Admin__c.isUpdateable() &&  (String.valueOf(tTracking.SR_Admin__c) == '') )) {
				System.debug('***  Entered value should not be empty ' );
				ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,'Entered value should not be empty'));
				
				return false;
			} else {
				return true;
			} 
		} else {
			ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,Label.TT_FillAtLeastOneField));
			
			return false;
		} 
		return false;
	}
	
	private PageReference returnNULL(){
		tTracking = new Time_Log__c();
        return null;
	}
    
    /*
    *   Class Name  :   CustomException
    *   Class Type  :   Exception
    */
    public class CustomException extends Exception {}
}