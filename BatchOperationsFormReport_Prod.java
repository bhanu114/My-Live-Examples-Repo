/*HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         =========== 
 |  05-APR-2016       BhanuPrakash    VirtuStream    Initial Creatoin
*/

global class BatchOperationsFormReport implements Database.Batchable<sObject>, Database.Stateful {
    
    public Date startDate = null;
    public Date endDate = null;
	public Date toStartMonthDate = null;
    global final String query = null;
    global String finalstr = null;
    global static List<String> monthHeader = null;
    global static Map<Integer,String> monthsMap = new Map<Integer,String> ();
    static List<PlNRsWrapper> plNRsWrapperList = new List<PlNRsWrapper>();
	Integer monthsCount = 0;
	String mmyyyy_head = null;
	String testOppId = null;
    //static Integer executionCount = 0;
    
    /*
    *   Constructor
    */
    global BatchOperationsFormReport(Date sD, Date eD){
        this.startDate = sD;
        this.endDate = eD;
        monthsCount = startDate.monthsBetween(endDate) +1;// Adding extra 1 as required to generate report accurately.
		String header = null;
        //Boolean isFirst = true;
        String[] csvHeaders = new List<String>();
		
        for(Integer i = 0; i < monthsCount; i++){
            mmyyyy_head = startDate.addMonths(i).month() + '-' + String.valueOf(startDate.addMonths(i).year());
            monthHeader.add(mmyyyy_head);
        }
		
        csvHeaders =  Label.CSV_Headers.split(',');
		
		//Other PL, RS and Opportunity fields header
        for(Integer i=0;i< csvHeaders.size();i++) {
            if(i==0){
                header = csvHeaders[i];
                //isFirst = false;
            }else{
                header += ',' + csvHeaders[i];
            }
        }
		//Revenue Month headers
        for(String mont : monthHeader){
            header += ','+mont;
        }
        
		 finalstr = header + '\n';
		toStartMonthDate = startDate.toStartOfMonth();
		testOppId = '0067A000002h28mQAA';
        query = 'SELECT Id,Opportunity__c, Product_Line__c, Period__c, Forecast_Amount__c, Actual__c,  Unique_Identifier__c FROM Revenue_Schedule__c where Period__c >= : startDate AND Period__c <= : endDate ORDER BY Opportunity__c'; 
    }
    /*
    *   Default Constructor
    */
    global BatchOperationsFormReport(){
        string header =  'Revenue Schedule Practice Line, Revenue Schedule Service Line,Opportunity ID, Probability (%), Legal Entity, Account Name,Opportunity, Job Code, Close Date, Start Date,Term, Owner Name, Currency ISO Code, Initial Revenue,Practice Line Forecast Amount,ACV Year 1,ACV Year 2,Opportunity Type,Sales Channel,Additional Account Relationship,Primary Data Center,Secondary Data Center,Lead Source,Additional Lead Sources,Heat Map,Theater, Segment, Sub-Segment,EMC Federated Opportunity, Forecasted';
        finalstr = header ;
    }
    /*
    *   Start Method
    */
    global Database.QueryLocator start(Database.BatchableContext BC) {
        return Database.getQueryLocator(query);
    }
    /*
    *   Execute method
    */
    global void execute(Database.BatchableContext BC, List<Revenue_Schedule__c> scope) {
        //executionCount++;
        string recordString = null;
        
        //********************************************* ACTUAL FUNCTIONALITY ***************************************************
            performScopeAction(scope);
        //*********************************************
    }
    /*
    *   Finish Method - End Mail to the user
    */
    global void finish(Database.BatchableContext BC) {
        Messaging.EmailFileAttachment csvAttc = new Messaging.EmailFileAttachment();
        blob csvBlob = Blob.valueOf(finalstr);
        string csvname= Label.Mail_Attachment_Name + '.csv';
        csvAttc.setFileName(csvname);
        csvAttc.setBody(csvBlob);
        Messaging.SingleEmailMessage email =new Messaging.SingleEmailMessage();
        String[] toAddresses = new List<string> {UserInfo.getUserEmail()};
		String subject = Label.Mail_Subject;
        email.setSubject(subject);
        email.setToAddresses( toAddresses );
        email.setPlainTextBody(Label.Dear + UserInfo.getName() + ',\n\n' + Label.Mail_body + '\n\n' + Label.Mail_Regards +',\n' + Label.Virtustream_Admin);
        email.setFileAttachments(new Messaging.EmailFileAttachment[]{csvAttc});
        Messaging.SendEmailResult [] r = Messaging.sendEmail(new Messaging.SingleEmailMessage[] {email});
    }
    
    static{
        monthHeader = new List<String>();
        monthsMap.put(1,'Jan');
        monthsMap.put(2,'Feb');
        monthsMap.put(3,'Mar');
        monthsMap.put(4,'Apr');
        monthsMap.put(5,'May');
        monthsMap.put(6,'Jun');
        monthsMap.put(7,'Jul');
        monthsMap.put(8,'Aug');
        monthsMap.put(9,'Sep');
        monthsMap.put(10,'Oct');
        monthsMap.put(11,'Nov');
        monthsMap.put(12,'Dec');
    }
    /*
    *   Perform Actions
    */
    public void performScopeAction(List<Revenue_Schedule__c> scope){
		Map<String,Revenue_Schedule__c> tempRSMap = new Map<String,Revenue_Schedule__c>();
		Map<Id, Map<String,Revenue_Schedule__c>> plIdNRSMap = new Map<Id, Map<String,Revenue_Schedule__c>>();
		Map<Id,Opportunity> oppsMap = new Map<Id,Opportunity>();
		Set<Id> oppIdSet = new Set<Id>();
		
        String mmyyyy = null;
        //String mmyyyy_head = null;
		
        //Integer monthsCount = startDate.monthsBetween(endDate) +1;// Adding extra 1 as required to generate report accurately.
        
        for(Integer i = 0; i < monthsCount; i++){
            mmyyyy_head = startDate.addMonths(i).month() + '-' + String.valueOf(startDate.addMonths(i).year());
            monthHeader.add(mmyyyy_head);
        }
		
            for(Revenue_Schedule__c rs : scope) {
                tempRSMap = new Map<String,Revenue_Schedule__c>();
                mmyyyy = rs.Period__c.month() + '-' + rs.Period__c.year();
                
                if(plIdNRSMap.get(rs.Product_Line__c) == null){
		             tempRSMap.put(mmyyyy,rs);
                    plIdNRSMap.put(rs.Product_Line__c,tempRSMap);
                } else {
		            tempRSMap = plIdNRSMap.get(rs.Product_Line__c);
                    tempRSMap.put(mmyyyy,rs);
                    plIdNRSMap.put(rs.Product_Line__c,tempRSMap);
                }
            }
			
		    Map<Id,Product_Line__c> plMap = new Map<Id,Product_Line__c>([Select Id, Name, Opportunity__c, Service_Line__c, Unique_Identifier__c,Job_Code__c,ACV_Year_1__c,ACV_Year_2__c,TCV__c,Start_Date__c,Term__c ,Practice_Line__c,Initial_Revenue__c,Opportunity__r.id,Opportunity__r.Probability,Opportunity__r.AccountId,Opportunity__r.OwnerId ,Opportunity__r.CloseDate,Opportunity__r.Legal_Entity__c,Opportunity__r.Additional_Lead_Sources__c,Opportunity__r.Forecasted__c,Opportunity__r.Heat_Map__c,Opportunity__r.EMC_Federated_Opportunity__c,Initial_Start_Date__c  from Product_Line__c where Id IN : plIdNRSMap.keySet()]);
            Map<Id,Id> plIdOpIdMap = new Map<Id,Id>();

            for(Id plId : plMap.keySet()){
                oppIdSet.add(plMap.get(plId).Opportunity__c);
                plIdOpIdMap.put(plId, plMap.get(plId).Opportunity__c);
            }
            
            oppsMap  = VisualizationHelper.getRelatedOpportunitiesById(oppIdSet);
			PlNRsWrapper plNRsWrapperTemp = null;
            List<Nodes> nodesList = new List<Nodes>();
            Nodes nod = null;
			
			//Iterate throgh all Product Line records we prepared above
			//And Create List of PlNRsWrapper wrapper class objects with Product Line Opportunity and Revenue Nodes by months selected
            for(Id oneId : plIdNRSMap.keySet()){
                plNRsWrapperTemp = new PlNRsWrapper();//To create list of records to show in UI
				
				//Process if each Product Line hase list of Revenue records
                if(plIdNRSMap.get(oneId).values() != null){
                    Map<String,Revenue_Schedule__c> tempRSMap1 = plIdNRSMap.get(oneId);
                    Set<String> chaildKeys = plIdNRSMap.get(oneId).keySet();
                    plNRsWrapperTemp.prodLineW=plMap.get(oneId);
					
					nodesList = null;
                    nodesList = new List<Nodes>();
					
                    if(oppsMap.keySet().contains(plIdOpIdMap.get(oneId))){
                        plNRsWrapperTemp.opp = oppsMap.get(plIdOpIdMap.get(oneId));//oppsMap.get(oneId);
                    }
                    //Integer mCount = 0;
                    for(String mDate : monthHeader){
                        nod = new Nodes();
                        if(chaildKeys.contains(mDate)){
                            nod.value =  (tempRSMap1.get(mDate).Forecast_Amount__c != null) ? tempRSMap1.get(mDate).Forecast_Amount__c : 0;
							
                        } else {
                            nod.value = 0;
			            }
                        nodesList.add(nod);
                       //mCount++;
                    }//End inner for
                    plNRsWrapperTemp.monthsReport = nodesList;
					
                } /*else {
					
                   // plNRsWrapperTemp.plName = plMap.get(oneId).name;
                    if(oppsMap.keySet().contains(oneId)){
                        plNRsWrapperTemp.opp = oppsMap.get(oneId);
                    }
                    for(Integer i = 0; i < monthsCount; i++){
                        nod = new Nodes();
                        nod.value = 0;
                        nodesList.add(nod);
                    }
                    plNRsWrapperTemp.monthsReport = nodesList;
                }*/
                plNRsWrapperList.add(plNRsWrapperTemp);
				//}//End else
            }//End Outer For
            
        if(!plNRsWrapperList.isEmpty()){
            for(PlNRsWrapper plw: plNRsWrapperList){
                String loopRow = check(plw.prodLineW.Practice_Line__c)+','
				+check(plw.prodLineW.Service_Line__c)+','
				+plw.prodLineW.Opportunity__r.id+','
				+plw.opp.Probability+','
				+check(plw.opp.Legal_Entity__c)+','
				+replace(plw.opp.Account.Name)+','
				+replace(check(plw.opp.Name))+','
				+check(plw.prodLineW.Job_Code__c)+','
				+plw.opp.CloseDate+','
				+plw.prodLineW.Start_Date__c+','
				+plw.prodLineW.Term__c +','
				+check(plw.opp.Owner.Name)+','
				+plw.opp.CurrencyIsoCode+','
				+checkD(plw.prodLineW.Initial_Revenue__c)+','
				+plw.prodLineW.TCV__c+','
				+plw.prodLineW.ACV_Year_1__c+','
				+plw.prodLineW.ACV_Year_2__c+','
				+check(plw.opp.Type)+','
				+check(plw.opp.Sales_Channel__c)+','
				+replace(check(plw.opp.Additional_Account_Relationship__r.Name))+','
				+check(plw.opp.Primary_Data_Center__c)+','
				+check(plw.opp.Secondary_Data_Center__c)+','
				+check(plw.opp.LeadSource)+','
				+check(plw.opp.Additional_Lead_Sources__c)+','
				+plw.opp.Heat_Map__c+','
				+check(plw.opp.Account.Theater__c)+','
				+check(plw.opp.Account.Segment__c)+','
				+check(plw.opp.Account.Sub_Segment__c)+','
				+plw.opp.EMC_Federated_Opportunity__c+','
				+plw.opp.Forecasted__c;
                
                for(Nodes nodz : plw.monthsReport){
                    loopRow += ','+String.valueOf(nodz.value);
                }
                loopRow += '\n';
                finalstr += loopRow;
				
            }
        } 
    }
    /*
    *   Replace null with empty value
    */
    public String check(String value){
        if(value == null){
            return '';
        }
        return value;
    }
    /*
    *   Replace decimal null with 0 value
    */
    public Decimal checkD(Decimal value){
        if(value == null){
            return 0;
        }
        return value;
    }
    /*
    *   Remove ',' in String and return
    */
	public String replace(String value){
        String[] values = value.split(',');
        String plainValue = '';
        for(String s : values){
            plainValue += s;
        }
        return plainValue;
    }
    /*public String replace(String value){
		if(value != null){
			//String[] values = value.split(',');
			//String plainValue = values[0];
			//for(Integer i=1;i<values.lenght;i++){
				//plainValue += ',' + values[i];
			//}
			if(value.contains(',')) {
				System.debug('"****** value has , = '+ value);
			}
			return  '\'' + value + '\'';
		}
		return '';
    }*/
    /*
    *   Wrapper class
    */
    public class PlNRsWrapper {
        public Transient Opportunity opp{get;set;}
        public Product_Line__c prodLineW{get;set;}
        public Transient List<Nodes> monthsReport { get; set; }//= null;        
        //public Transient String plName{ get; set; }// = null;
    }
    /*
    *   Wrapper class
    */
    public class Nodes {
        public Transient Decimal value { get; set; }//= null;
    }
}