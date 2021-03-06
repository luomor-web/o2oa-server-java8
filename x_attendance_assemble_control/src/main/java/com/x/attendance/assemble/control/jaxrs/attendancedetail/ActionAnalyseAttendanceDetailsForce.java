package com.x.attendance.assemble.control.jaxrs.attendancedetail;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonElement;
import com.x.attendance.assemble.control.ExceptionWrapInConvert;
import com.x.attendance.assemble.control.jaxrs.attendancedetail.ActionReciveSingleAttendance.Wi;
import com.x.attendance.assemble.control.processor.monitor.StatusSystemImportOpt;
import com.x.attendance.assemble.control.processor.sender.SenderForAnalyseData;
import com.x.attendance.entity.AttendanceStatisticalCycle;
import com.x.attendance.entity.AttendanceWorkDayConfig;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.ListTools;

public class ActionAnalyseAttendanceDetailsForce extends BaseAction {
	
	private static  Logger logger = LoggerFactory.getLogger( ActionAnalyseAttendanceDetails.class );
	
	protected ActionResult<Wo> execute( HttpServletRequest request, EffectivePerson effectivePerson, JsonElement jsonElement) throws Exception {
		ActionResult<Wo> result = new ActionResult<>();
		//List<String> personNames = null;
		List<AttendanceWorkDayConfig> attendanceWorkDayConfigList = null;
		Map<String, Map<String, List<AttendanceStatisticalCycle>>> topUnitAttendanceStatisticalCycleMap = null;
		StatusSystemImportOpt statusSystemImportOpt = StatusSystemImportOpt.getInstance();
		Boolean check = true;
		Wi wrapIn = null;
		
		try {
			wrapIn = this.convertToWrapIn(jsonElement, Wi.class);
		} catch (Exception e) {
			check = false;
			Exception exception = new ExceptionWrapInConvert(e, jsonElement);
			result.error(exception);
			logger.error(e, effectivePerson, request, null);
		}
		
		if( statusSystemImportOpt.getProcessing() ) {
			check = false;
			Exception exception = new ExceptionAttendanceDetailProcess( "????????????????????????????????????????????????????????????......" );
			result.error(exception);
		}
		List<String> personNames = null;
		String startDate = wrapIn.getStartDate(); 
		String endDate = wrapIn.getEndDate();
		Boolean forceFlag = wrapIn.getForceFlag();
		personNames = wrapIn.getPersonNames();

		if (check) {
			try {
				if(ListTools.isEmpty(personNames)){
					//personNames = attendanceDetailServiceAdv.getAllAnalysenessPersonNames( startDate, endDate );
					personNames = attendanceDetailServiceAdv.getAllAnalysenessPersonNamesForce( startDate, endDate ,forceFlag);
					if( personNames == null || personNames.isEmpty() ) {
						check = false;
						Exception exception = new ExceptionAttendanceDetailProcess( "?????????????????????????????????????????????." + "????????????:" + startDate + ", ????????????:" + endDate );
						result.error(exception);
					}
				}
				
			} catch (Exception e) {
				check = false;
				Exception exception = new ExceptionAttendanceDetailProcess(e, "?????????????????????????????????????????????????????????????????????????????????????????????." + "????????????:" + startDate + ", ????????????:" + endDate );
				result.error(exception);
				logger.error(e, effectivePerson, request, null);
			}
		}
		
		if ( check ) {
			try {
				attendanceWorkDayConfigList = attendanceWorkDayConfigServiceAdv.listAll();
			} catch (Exception e) {
				check = false;
				Exception exception = new ExceptionAttendanceDetailProcess( e, "???????????????ID???????????????????????????????????????????????????????????????" );
				result.error(exception);
				logger.error(e, effectivePerson, request, null);
			}
		}
		if ( check ) {
			try {// ???????????????????????????????????????Map
				topUnitAttendanceStatisticalCycleMap = attendanceStatisticCycleServiceAdv.getCycleMapFormAllCycles( effectivePerson.getDebugger() );
			} catch (Exception e) {
				check = false;
				Exception exception = new ExceptionAttendanceDetailProcess( e, "???????????????????????????????????????????????????????????????." );
				result.error(exception);
				logger.error(e, effectivePerson, request, null);
			}
		}
		if ( check ) {
			new SenderForAnalyseData().executeForce( personNames, startDate, endDate,forceFlag, attendanceWorkDayConfigList, topUnitAttendanceStatisticalCycleMap, effectivePerson.getDebugger() ); 
		}
		return result;
	}
	
	public static class Wi {

		@FieldDescribe( "????????????." )
		private String startDate = null;

		@FieldDescribe( "????????????." )
		private String endDate = null;

		@FieldDescribe("??????distinguishedName.")
		private List<String>personNames;

		@FieldDescribe("???????????????????????????????????????.")
		private Boolean forceFlag;


		public String getStartDate() {
			return startDate;
		}
		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}
		public String getEndDate() {
			return endDate;
		}
		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}
		public List<String> getPersonNames() {
			return personNames;
		}
		public void setPersonNames(List<String> personNames) {
			this.personNames = personNames;
		}
		public Boolean getForceFlag() {
			return forceFlag;
		}
		public void setForceFlag(Boolean forceFlag) {
			this.forceFlag = forceFlag;
		}
		
	}
	
	public static class Wo extends WoId {
		public Wo( String id ) {
			setId( id );
		}
	}
}