/*HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         =========== 
 |  10-JAN-2016       BhanuPrakash    VirtuStream    Initial Creatoin
*/
public class  ProductLineHelper {
    private static String MANUAL = 'Manual';
    private static String EVEN = 'Even';
    public static Boolean runOnce = true;
    public static final String ADDITION = 'Addition';
    
    /*
    *   Create new Revenue_Schedule__c records based on Initial_Term__c value on Product_Line__c after insert
    */
    public static void createRevenueRecords(Map<Id,Product_Line__c> newPLMap){
        if(runOnce){
            runOnce = false;
            List<Product_Line__c> updatablePLList = new List<Product_Line__c>();
            Set<Id> updatablePLIdset = new Set<Id>();
            Set<Id> updatableOppIdset = new Set<Id>();
            Map<Id,Id> plIdOpIdMap = new Map<Id,Id>();
            
            Boolean isException = false;
            
            for(Product_Line__c pl : newPLMap.values()){
                if(pl.Generate_Schedule__c){
                    updatablePLList.add(pl);
                    updatablePLIdset.add(pl.Id);
                    updatableOppIdset.add(pl.Opportunity__c);
                    plIdOpIdMap.put(pl.Id, pl.Opportunity__c);
                }
            }
            
            Set<Id> opIDSet = new Set<ID>();
            opIDSet.addAll(plIdOpIdMap.values());
            
            //On the calculation used for ACV it should be 13 Months from the Close Date of the opportunity
            Map<id, Opportunity> oppIdOppMap = new Map<Id, Opportunity>( [SELECT Id, CloseDate FROM Opportunity WHERE ID IN : plIdOpIdMap.values()]);
            Map<Id, Date> plIdOppDateMap = new Map<Id, Date>();
            
            for( Id plId : plIdOpIdMap.keySet()) {
                if(oppIdOppMap.get(plIdOpIdMap.get(plId)) != null){
                    plIdOppDateMap.put(plId, oppIdOppMap.get(plIdOpIdMap.get(plId)).CloseDate);
                }
            }
            
            if(!updatablePLList.isEmpty()){
                
                List<Revenue_Schedule__c> rsInsertList = new List<Revenue_Schedule__c>();
                Decimal tempAnualContactValue1 = 0;
                Decimal tempAnualContactValue2 = 0;
                Decimal salesProj = null;
                Revenue_Schedule__c rs = null;
                Integer loopCount = 0;
                
                for(Product_Line__c pl : updatablePLList){
                    //RESET LOOP VARIABLES
                    loopCount = 0;
                    salesProj = 0.0;
                    rs = null;
                    //acvw = null;
                    tempAnualContactValue1 = 0;
                    tempAnualContactValue1 = 0;
                    tempAnualContactValue2 = 0;
                    Date startDate = null;
                    
                    //GENERAGE START DATE FORM OPPORTUNITY CLOSE DATE
                    if(plIdOppDateMap.get(pl.Id) != null){
                        startDate = plIdOppDateMap.get(pl.Id);
                    }
                    
                    if(pl.Spread_Type__c.equals(MANUAL)){
                        salesProj = (Decimal) pl.Initial_Revenue__c;
                        for(Integer i=0; i<pl.Initial_Term__c; i++){
                            rs = new Revenue_Schedule__c();
                            rs.CurrencyIsoCode = pl.CurrencyIsoCode;
                            rs.Product_Line__c = pl.Id;
                            rs.Opportunity__c = pl.Opportunity__c;
                            rs.Opportunity_Close_Date__c = plIdOppDateMap.get(pl.Id);//Populate Opportunity Close date
                            rs.Period__c = pl.Initial_Start_Date__c.addMonths(i);
                            rs.Projection__c = null;//salesProj;
                            rsInsertList.add(rs);
                        }
                    } else 
                    if(pl.Spread_Type__c.equals(EVEN)){
                        salesProj = (Decimal) (pl.Initial_Revenue__c/pl.Initial_Term__c);
                        //added by Partha: To set projection values for Revenue schedules to be in correct format
                        salesProj = salesProj.setScale(2);
                        
                        for(Integer i=0; i < pl.Initial_Term__c; i++){
                            rs = new Revenue_Schedule__c();
                            rs.Product_Line__c = pl.Id;
                            rs.CurrencyIsoCode = pl.CurrencyIsoCode;
                            rs.Opportunity__c = pl.Opportunity__c;
                            //Setting Opportunity cloase date for RS record
                            rs.Opportunity_Close_Date__c = plIdOppDateMap.get(pl.Id);//Populate Opportunity Close date
                            rs.Period__c = pl.Initial_Start_Date__c.addMonths(i);
                            rs.Projection__c = salesProj;
                            rsInsertList.add(rs);
                            loopCount++;//Starts form 1
                        }
                    }//END inner for
                }//END inner for
                //Insert Child RS records
                if(rsInsertList.size() != 0){
                    try{
                        Database.insert(rsInsertList);
                    } catch(Exception e){
                        isException = true;
                        System.debug('############# Exception : ' + e.getMessage());
                    }
                }
                if(!isException){// IF NO EXCEPTION ON THE EVENT OF INSERTING REVENUE SCHEDULE RECORDS
                    uncheckGeneratedPracticeLines(updatablePLIdset);
                    
                    //GET RELATED OPPORTUNITIES ANNUAL CONTACT VALUE FIELD VALUES
                    //Depricated as implemented Roll-up summary convept
                    //updateOpptyAnnuaContactValues(updatableOppIdset, oppIdNACVWMap, ADDITION);
                }
            }//end if
            
        }
    }
    
    /*
    *   update existing Revenue_Schedule__c records based on changed value on Product_Line__c after update
    */
    public static void updateRevenueRecords(Map<Id,Product_Line__c> oldPLMap,Map<Id,Product_Line__c> newPLMap){
        if(runOnce){
            runOnce = false;
            //PracticeLineHelper.PracticeLineWrapper plw = null;
            Map<Id,Product_Line__c> plwMap = new Map<Id,Product_Line__c>();
            //List<PracticeLineHelper.PracticeLineWrapper> tempWrappers = new List<PracticeLineHelper.PracticeLineWrapper>();
            Set<Id> updatablePLIdset = new Set<Id>();
            Set<Id> updatableOppIdset = new Set<Id>();
            Boolean isException = false;
            Map<Id,Id> plIdOpIdMap = new Map<Id,Id>();
            
//**********************    FILTER TO POINT PRACTICE LINE ITEMS TO WORK AHEAD - START  *****************
            for(Product_Line__c pl : newPLMap.values()){
                //if Generate and change in recenue or start date
                if(pl.Generate_Schedule__c ){
                    //Collect PL ids to update Generate as false
                    updatablePLIdset.add(pl.Id);
                    updatableOppIdset.add(pl.Opportunity__c);
                    plIdOpIdMap.put(pl.Id, pl.Opportunity__c);
                    plwMap.put(pl.Id,pl);
                    //}//End If or condition
                }//End Generate Schedule
            }
            
            
//**********************    FILTER TO POINT PRACTICE LINE ITEMS TO WORK AHEAD - END  *****************
            
            //CHEDK IS REVENUE RECORDS ARE EXISTING FOR ALL PRACTICE RECORDS, IF NOT FIND UPDATABLE AND CREATABLE REVENUE PARENT PRACTICE RECORDS AND PERFOM RELAVENT OPERATIONS
           List<Revenue_Schedule__c> allRevScheList = [SELECT Product_Line__c,Projection__c  From Revenue_Schedule__c where Product_Line__c IN :plwMap.keySet()];
            Map<Id,Product_Line__c> pLsToCreateRSMap = null;
            Set<Id> plIdsWithChaildSet = new Set<Id>();
            
            //IF NON-OF THE PRACTICE RECORD HAD REVENUE RECORD THEN CREATE REVENUE RECORDS FOR ALL PL RECORDS
            if(allRevScheList.size() == 0){
                runOnce = true;
                createRevenueRecords(newPLMap);
            } else {//IF SOME PL RECORDS HAVING CHAILDS AND SOME PL RECORDS NOT HAVING, SEPERATE THE RECORDS ACCORDS ACCORDING TO.
                pLsToCreateRSMap = new Map<Id,Product_Line__c>();
                
                //SEPERATE all PRACTICE RECORD ID'S WITH CHILD
                for(Revenue_Schedule__c rs :  allRevScheList){
                    plIdsWithChaildSet.add(rs.Product_Line__c);
                }
                //GET PL RECORD IDS WITHOUT CHAILDS AMONG ALL
                for(Id oneId : plwMap.keySet()){
                    if(!plIdsWithChaildSet.contains(oneId)){
                        pLsToCreateRSMap.put(oneId, plwMap.get(oneId));
                    }
                }
            }
            
            if(pLsToCreateRSMap != null && !pLsToCreateRSMap.isEmpty()){
                runOnce = true;
                createRevenueRecords(newPLMap);
            }
            
           if(!plIdsWithChaildSet.isEmpty()) {
                List<Revenue_Schedule__c> rsUpdatetList = new List<Revenue_Schedule__c>();
                List<Product_Line__c> updatablePLList = new List<Product_Line__c>();
                Decimal tempAnualContactValue1 = 0;
                Decimal tempAnualContactValue2 = 0;
                
                Map<Id,List<Revenue_Schedule__c>> plIdnRSListMap = new Map<Id,List<Revenue_Schedule__c>>();
                List<Revenue_Schedule__c> tempRSList = new List<Revenue_Schedule__c>();
                Decimal projectionDefValue = 0;
                
                //Group Revenue Schedule records by PracticeLine ID
                for(Revenue_Schedule__c rs1 : allRevScheList){
                    if(plIdnRSListMap.get(rs1.Product_Line__c) == null){
                        tempRSList.clear();
                        tempRSList.add(rs1);
                        plIdnRSListMap.put(rs1.Product_Line__c,tempRSList);
                    }else {
                        tempRSList = plIdnRSListMap.get(rs1.Product_Line__c);
                        tempRSList.add(rs1);
                        plIdnRSListMap.put(rs1.Product_Line__c,tempRSList);
                    }
                }
                
                //On the calculation used for ACV it should be 13 Months from the Close Date of the opportunity
                Map<id, Opportunity> oppIdOppMap = new Map<Id, Opportunity>( [SELECT Id, CloseDate FROM Opportunity WHERE ID IN : plIdOpIdMap.values()]);
                Map<Id, Date> plIdOppDateMap = new Map<Id, Date>();
                
                for( Id plId : plIdOpIdMap.keySet()) {
                    if(oppIdOppMap.get(plIdOpIdMap.get(plId)) != null){
                        plIdOppDateMap.put(plId, oppIdOppMap.get(plIdOpIdMap.get(plId)).CloseDate);
                    }
                }
                
                for(Product_Line__c pl : plwMap.values()){
                    Decimal salesProj = 0.0;
                    Revenue_Schedule__c rs = new Revenue_Schedule__c();
                    Integer loopCount = 0;
                    tempAnualContactValue1 = 0;
                    tempAnualContactValue2 = 0;
                    Date startDate = null;
                    
                    //GENERAGE START DATE FORM OPPORTUNITY CLOSE DATE
                    if(plIdOppDateMap.get(pl.Id) != null){
                        startDate = plIdOppDateMap.get(pl.Id);
                    }
                
                    if(pl.Spread_Type__c.equals(MANUAL)){
                        Integer mCount = 0;
                        salesProj = (Decimal) pl.Initial_Revenue__c;//If Revenue typ is 'Per Month'
                        for(Revenue_Schedule__c rs2 : plIdnRSListMap.get(pl.Id)){
                            rs.Product_Line__c = pl.Id;
                            rs.Opportunity__c = pl.Opportunity__c;
                            rs.CurrencyIsoCode = pl.CurrencyIsoCode;
                            rs2.Period__c = pl.Initial_Start_Date__c.addMonths(mCount++);
                            rs2.Projection__c = null;//salesProj;
                            rsUpdatetList.add(rs2);
                        }
                        
                    } else if(pl.Spread_Type__c.equals(EVEN)){//
                        salesProj = (Decimal) (pl.Initial_Revenue__c/pl.Initial_Term__c);
                        //added by Partha: To set projection values for Revenue schedules to be in correct format
                        salesProj = salesProj.setScale(2);
                        Integer mCount = 0;
                        for(Revenue_Schedule__c rs2 : plIdnRSListMap.get(pl.Id)){
                            if(rs2 != null){
                                rs.Product_Line__c = pl.Id;
                                rs.Opportunity__c = pl.Opportunity__c;
                                rs.CurrencyIsoCode = pl.CurrencyIsoCode;
                                rs2.Period__c = pl.Initial_Start_Date__c.addMonths(mCount++);
                                projectionDefValue = (salesProj-rs2.Projection__c);
                                rs2.Projection__c = salesProj;
                                rsUpdatetList.add(rs2);
                            }
                        }
                    }//end else
                    //}//end outer for
                }//End if
                if(!rsUpdatetList.isEmpty()){
                    try{
                        Database.update(rsUpdatetList);
                    }catch(Exception e){
                        isException = true;
                        System.debug('############# Exception : ' + e.getMessage());
                    }
                }
                if(!isException){
                    uncheckGeneratedPracticeLines(updatablePLIdset);
                    //GET RELATED OPPORTUNITIES ANNUAL CONTACT VALUE FIELD VALUES
                    //Depricated as implemented Roll-up summary convept
                    //updateOpptyAnnuaContactValues(updatableOppIdset, oppIdNACVWMap, ADDITION);
                }
                //}
            }// PL RECORDS WITH CHAILDS UPDATE
        }//End Once
    }
    
    public static void setProductLineCorrency(List<Product_Line__c> plList){
        Set<Id> oppIdset = new Set<Id>();
        Map<Id, Opportunity> oppIdOppMap = null;
        for( Product_Line__c pl : plList){
            oppIdset.add(pl.Opportunity__c);
        }
        if(!oppIdset.isEmpty()){
            oppIdOppMap = new Map<Id, Opportunity>([SELECT Id, CurrencyIsoCode FROM Opportunity WHERE Id IN :oppIdset ]);
            
            if(oppIdOppMap != null && !oppIdOppMap.isEmpty()){
                for( Product_Line__c pl : plList){
                    if(oppIdOppMap.get(pl.Opportunity__c) != null){
                        pl.CurrencyIsoCode = oppIdOppMap.get(pl.Opportunity__c).CurrencyIsoCode;
                    }
                }
            }
        }
    }
    
    /*
    *   Method to uncheck Gene
    */
    public static Boolean uncheckGeneratedPracticeLines(Set<Id> pLIdSe){
        //Flip Generate_Schedule__c filed to false for Practice Line record - Start
                if(pLIdSe.size() > 0){
                    List<Product_Line__c> updatablePLList = [Select Id, Generate_Schedule__c,Initial_Revenue__c,Initial_Start_Date__c,Initial_Term__c,Spread_Type__c From Product_Line__c where Id IN : pLIdSe];
                    for(Integer i=0; i< updatablePLList.size(); i++){
                        updatablePLList[i].Generate_Schedule__c = false;
                    }
                    if(updatablePLList.size() > 0){
                        try{
                            Database.update(updatablePLList);
                            return true;
                        }catch(Exception e){
                            System.debug('############# Exception : ' + e.getMessage());
                            return false;
                        }
                    }
                }
            //Flip Generate_Schedule__c filed to false for Practice Line record - END
        return false;
    }
    /*
    *   Method to perform before delete actions calling from Trigger
    */
    public static void doBeforeDeleteOperations(Map<Id,Product_Line__c> newPlMap){
        //Operation 1 : Add error to ProductLine records if related Opportunity StageName value is '100% Won (closed/won)' - START
        //bipass System admin profiles
        Set<String> sysIdSet = new Set<String>(VirtuStream_Settings__c.getInstance().System_Admin_IDs__c.split(','));
        if(!sysIdSet.contains(UserInfo.getProfileId().subString(0,15))){
            String won_closed  = Opportunity_Stage_Names__c.getInstance().Hundred_Percent_Won_Lost__c;
            String lost_closed  = Opportunity_Stage_Names__c.getInstance().Zero_Percent_Lost_Closed__c;
            String stageValues = won_closed + ',' + lost_closed;
            if(!String.isEmpty(stageValues)){
                GenericOperationsHelper.addErrByParentFieldValue((Map<Id,Sobject>)newPlMap, 'Opportunity', 'Opportunity__c', 'StageName', stageValues, Label.Err_Msg_Cont_delete_ProductLineOrSchedule);
                //Operation 1 : Add error to ProductLine records if related Opportunity StageName value is '100% Won (closed/won)' - END
            }
        }
    }
	public static Boolean pushRevenueSchedulePeriod(String oppId, Integer  pushMonthNo){
		List<Revenue_Schedule__c> rsList = new List<Revenue_Schedule__c>();
		rsList = [SELECT ID, Period__c FROM Revenue_Schedule__c WHERE Opportunity__c=: oppId LIMIT 50000];
		
		if(rsList.size() > 0){
			//Push Revenue schedule Preiod as per months mentioned
			for(Revenue_Schedule__c rs : rsList){
				rs.Period__c = rs.Period__c.addMonths(pushMonthNo);
			}
			try {
				Database.update(rsList);
				return true;
			}catch(Exception ex){
				return false;
			}
		}
		return false;
	}
}