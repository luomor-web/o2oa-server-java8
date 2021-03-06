package com.x.attendance.assemble.control.jaxrs.attendancedetail;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.x.attendance.entity.AttendanceAppealAuditInfo;
import com.x.attendance.entity.AttendanceAppealInfo;
import com.x.attendance.entity.AttendanceScheduleSetting;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.tools.ListTools;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonElement;
import com.x.attendance.assemble.control.Business;
import com.x.attendance.assemble.control.ExceptionWrapInConvert;
import com.x.attendance.assemble.control.service.AttendanceEmployeeConfigServiceAdv;
import com.x.attendance.assemble.control.service.UserManagerService;
import com.x.attendance.entity.AttendanceDetail;
import com.x.attendance.entity.AttendanceEmployeeConfig;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.StandardJaxrsAction;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;

public class ActionListNextWithFilter extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionListNextWithFilter.class);
	private UserManagerService userManagerService = new UserManagerService();
	protected AttendanceEmployeeConfigServiceAdv attendanceEmployeeConfigServiceAdv = new AttendanceEmployeeConfigServiceAdv();

	protected ActionResult<List<Wo>> execute(HttpServletRequest request, EffectivePerson effectivePerson, String id,
			Integer count, JsonElement jsonElement) throws Exception {
		ActionResult<List<Wo>> result = new ActionResult<>();
		List<Wo> wraps = new ArrayList<>();
		EffectivePerson currentPerson = this.effectivePerson(request);
		long total = 0;
		List<AttendanceDetail> detailList = null;
		List<String> topUnitNames = new ArrayList<String>();
		List<String> unitNames = new ArrayList<String>();
		List<String> topUnitNames_tmp = null;
		List<String> unitNames_tmp = null;
		Wi wrapIn = null;
		AttendanceScheduleSetting scheduleSetting_top = null;
		AttendanceScheduleSetting scheduleSetting = null;
		Boolean check = true;
		
		List<String> unUnitNameList = new ArrayList<String>();
		List<String> personNameList = new ArrayList<String>();

		try {
			wrapIn = this.convertToWrapIn(jsonElement, Wi.class);
		} catch (Exception e) {
			check = false;
			Exception exception = new ExceptionWrapInConvert(e, jsonElement);
			result.error(exception);
			logger.error(e, currentPerson, request, null);
		}
		if (check) {
			try {
				EntityManagerContainer emc = EntityManagerContainerFactory.instance().create();
				Business business = new Business(emc);

				// ?????????ID??????????????????sequence
				Object sequence = null;
				if (id == null || "(0)".equals(id) || id.isEmpty()) {
				} else {
					if (!StringUtils.equalsIgnoreCase(id, StandardJaxrsAction.EMPTY_SYMBOL)) {
						sequence = PropertyUtils.getProperty(emc.find(id, AttendanceDetail.class),  JpaObject.sequence_FIELDNAME);
					}
				}

				// ???????????????????????????????????????????????????
				if ( StringUtils.isNotEmpty( wrapIn.getQ_topUnitName() )) {
					topUnitNames.add(wrapIn.getQ_topUnitName());
					scheduleSetting_top = attendanceScheduleSettingServiceAdv.getAttendanceScheduleSettingWithUnit(wrapIn.getQ_topUnitName(), effectivePerson.getDebugger() );
					try {
						topUnitNames_tmp = userManagerService.listSubUnitNameWithParent(wrapIn.getQ_topUnitName());
					} catch (Exception e) {
						Exception exception = new ExceptionAttendanceDetailProcess(e,
								"???????????????????????????????????????????????????????????????????????????TopUnit:" + wrapIn.getQ_topUnitName());
						result.error(exception);
						logger.error(e, currentPerson, request, null);
					}
					if (topUnitNames_tmp != null && topUnitNames_tmp.size() > 0) {
						for (String topUnitName : topUnitNames_tmp) {
							topUnitNames.add(topUnitName);
						}
					}
					wrapIn.setTopUnitNames(topUnitNames);
				}

				// ??????????????????,??????????????????
				if ( StringUtils.isNotEmpty( wrapIn.getQ_unitName() )) {
					unitNames.add(wrapIn.getQ_unitName());
					scheduleSetting = attendanceScheduleSettingServiceAdv.getAttendanceScheduleSettingWithUnit(wrapIn.getQ_unitName(), effectivePerson.getDebugger() );
					try {
						unitNames_tmp = userManagerService.listSubUnitNameWithParent(wrapIn.getQ_unitName());
					} catch (Exception e) {
						Exception exception = new ExceptionAttendanceDetailProcess(e,
								"???????????????????????????????????????????????????????????????Unit:" + wrapIn.getQ_unitName());
						result.error(exception);
						logger.error(e, currentPerson, request, null);
					}
					if (unitNames_tmp != null && unitNames_tmp.size() > 0) {
						for (String unitName : unitNames_tmp) {
							unitNames.add(unitName);
						}
					}
					wrapIn.setUnitNames(unitNames);
				}

				if (check ) {
					unUnitNameList = getUnUnitNameList();
					personNameList = getUnPersonNameList();
					// ??????????????????????????????????????????????????????
					//detailList = business.getAttendanceDetailFactory().listIdsNextWithFilter(id, count, sequence, wrapIn);
					detailList = business.getAttendanceDetailFactory().listIdsNextWithFilterUn(id, count, sequence, wrapIn,unUnitNameList,personNameList);
					// ????????????????????????????????????????????????
					//total = business.getAttendanceDetailFactory().getCountWithFilter(wrapIn);
					total = business.getAttendanceDetailFactory().getCountWithFilterUn(wrapIn,unUnitNameList,personNameList);
					// ??????????????????????????????????????????????????????????????????????????????????????????
					wraps = Wo.copier.copy(detailList);
				}

				if( scheduleSetting == null ){
					scheduleSetting = scheduleSetting_top;
				}

				if (check && ListTools.isNotEmpty( wraps )) {
					Integer signProxy = 1;
					List<AttendanceAppealInfo> appealInfos = null;
					AttendanceAppealAuditInfo appealAuditInfo = null;
					List<WoAttendanceAppealInfo> woAppealInfos = null;
					for( Wo detail : wraps ){
						if ( scheduleSetting != null ) {
							signProxy = scheduleSetting.getSignProxy();
						}
						detail.setSignProxy( signProxy );

						//???????????????????????????
						if( detail.getAppealStatus() != 0 ){
							//??????????????????????????????????????????????????????
							appealInfos = attendanceAppealInfoServiceAdv.listWithDetailId( detail.getId() );
							if(ListTools.isNotEmpty( appealInfos ) ){
								woAppealInfos = WoAttendanceAppealInfo.copier.copy( appealInfos );
							}
							if(ListTools.isNotEmpty( woAppealInfos ) ){
								for( WoAttendanceAppealInfo woAppealInfo : woAppealInfos ){
									appealAuditInfo = attendanceAppealInfoServiceAdv.getAppealAuditInfo( woAppealInfo.getId() );
									if( appealAuditInfo != null ){
										woAppealInfo.setAppealAuditInfo( WoAttendanceAppealAuditInfo.copier.copy( appealAuditInfo ));
									}
								}
							}
							detail.setAppealInfos(woAppealInfos);
						}
					}
				}
			} catch (Throwable th) {
				th.printStackTrace();
				result.error(th);
			}
		}
		result.setCount(total);
		result.setData(wraps);
		return result;
	}

	public static class Wi extends WrapInFilter{

	}

	public static class Wo extends AttendanceDetail {

		private static final long serialVersionUID = -5076990764713538973L;

		@FieldDescribe("??????????????????????????????????????????1-????????????????????????????????????????????? 2-????????????????????????????????????????????????????????????????????? 3-???????????????????????????????????????????????????")
		private Integer signProxy = 1;

		@FieldDescribe("??????????????????")
		private List<WoAttendanceAppealInfo> appealInfos = null;

		public List<WoAttendanceAppealInfo> getAppealInfos() { return appealInfos; }

		public void setAppealInfos(List<WoAttendanceAppealInfo> appealInfos) { this.appealInfos = appealInfos; }

		public Integer getSignProxy() {
			return signProxy;
		}

		public void setSignProxy(Integer signProxy) {
			this.signProxy = signProxy;
		}

		public static WrapCopier<AttendanceDetail, Wo> copier = WrapCopierFactory.wo(AttendanceDetail.class, Wo.class,
				null, JpaObject.FieldsInvisible);
	}
	
	/**
	 * ??????????????????????????????
	 * @return
	 * @throws Exception 
	 */
	protected  List<String> getUnUnitNameList() throws Exception {
		List<String> unUnitNameList = new ArrayList<String>();

		List<AttendanceEmployeeConfig> attendanceEmployeeConfigs = attendanceEmployeeConfigServiceAdv.listByConfigType("NOTREQUIRED");

		if(ListTools.isNotEmpty(attendanceEmployeeConfigs)){
			for (AttendanceEmployeeConfig attendanceEmployeeConfig : attendanceEmployeeConfigs) {
				String unitName = attendanceEmployeeConfig.getUnitName();
				String employeeName = attendanceEmployeeConfig.getEmployeeName();

				if(StringUtils.isEmpty(employeeName) && StringUtils.isNotEmpty(unitName)){
					unUnitNameList.add(unitName);
					List<String> tempUnitNameList = userManagerService.listSubUnitNameWithParent(unitName);
					if(ListTools.isNotEmpty(tempUnitNameList)){
						for(String tempUnit:tempUnitNameList){
							if(!ListTools.contains(unUnitNameList, tempUnit)){
								unUnitNameList.add(tempUnit);
							}
						}
					}
				}
			} 
		}
		return unUnitNameList;
	}
	
	/**
	 * ??????????????????????????????
	 * @return
	 * @throws Exception 
	 */
	protected  List<String> getUnPersonNameList() throws Exception {
		List<String> personNameList = new ArrayList<String>();
		List<AttendanceEmployeeConfig> attendanceEmployeeConfigs = attendanceEmployeeConfigServiceAdv.listByConfigType("NOTREQUIRED");

		if(ListTools.isNotEmpty(attendanceEmployeeConfigs)){
			for (AttendanceEmployeeConfig attendanceEmployeeConfig : attendanceEmployeeConfigs) {
				String employeeName = attendanceEmployeeConfig.getEmployeeName();

				if(StringUtils.isNotEmpty(employeeName) && !ListTools.contains(personNameList, employeeName)){
					personNameList.add(employeeName);
				}
			}
		}
		return personNameList;
	}

}