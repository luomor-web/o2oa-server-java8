package com.x.cms.assemble.control.jaxrs.categoryinfo;

import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.cache.Cache;
import com.x.base.core.project.cache.CacheManager;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.ListTools;
import com.x.cms.core.entity.AppInfo;
import com.x.cms.core.entity.CategoryInfo;
import com.x.cms.core.entity.Document;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActionQueryGetControl extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionQueryGetControl.class);

	/**
	 * "control": {
      "allowVisit": false,
      "allowPublish": false,
      "allowManage": false
    },
	 * @param request
	 * @param id
	 * @param effectivePerson
	 * @return
	 * @throws Exception
	 */
	protected ActionResult<Wo> execute( HttpServletRequest request, String id, EffectivePerson effectivePerson )
			throws Exception {
		ActionResult<Wo> result = new ActionResult<>();
		Wo wo = new Wo();
		WoControl woControl = new WoControl();
		AppInfo appInfo = null;
		CategoryInfo categoryInfo = null;
		Boolean check = true;
		Boolean isManager = userManagerService.isManager(effectivePerson);
		Boolean isAnonymous = effectivePerson.isAnonymous();
		String personName = effectivePerson.getDistinguishedName();
		
		if ( StringUtils.isEmpty(id)) {
			check = false;
			Exception exception = new ExceptionCategoryIdEmpty();
			result.error(exception);
		}

		Cache.CacheKey cacheKey = new Cache.CacheKey( this.getClass(), personName, id, isAnonymous, isManager );
		Optional<?> optional = CacheManager.get(cacheCategory, cacheKey );

		if (optional.isPresent()) {
			result.setData((Wo)optional.get());
		} else {
			woControl = new WoControl();
			if( check ){
				try {
					categoryInfo = categoryInfoServiceAdv.getWithFlag( id );
					if( categoryInfo == null ){
						check = false;
						Exception exception = new ExceptionCategoryInfoNotExists( id );
						result.error( exception );
					}
				} catch (Exception e) {
					check = false;
					Exception exception = new ExceptionCategoryInfoProcess( e, "??????ID??????????????????????????????????????????ID:" + id );
					result.error( exception );
					logger.error( e, effectivePerson, request, null);
				}
			}
			if( check ){
				try {
					appInfo = appInfoServiceAdv.get( categoryInfo.getAppId() );
					if( appInfo == null ){
						check = false;
						Exception exception = new ExceptionAppInfoNotExists( categoryInfo.getAppId() );
						result.error( exception );
					}
				} catch (Exception e) {
					check = false;
					Exception exception = new ExceptionCategoryInfoProcess( e, "????????????id????????????????????????????????????????????????ID:" + categoryInfo.getAppId() );
					result.error( exception );
					logger.error( e, effectivePerson, request, null);
				}
			}

			if( check ){
				if( isManager ){
					woControl.setAllowManage( true );
					woControl.setAllowPublish( true );
					woControl.setAllowVisit( true );
				}else if( isAnonymous ) {
					if( appInfo.getAllPeopleView() && categoryInfo.getAllPeopleView() ){
						woControl.setAllowVisit( true );
					}
					if( appInfo.getAllPeoplePublish() && categoryInfo.getAllPeoplePublish() ){
						woControl.setAllowPublish( true );
					}
				}else{
					List<String> unitNames = userManagerService.listUnitNamesWithPerson( personName );
					List<String> groupNames = userManagerService.listGroupNamesByPerson( personName );
					if( unitNames == null ){ unitNames = new ArrayList<>(); }
					if( groupNames == null ){ groupNames = new ArrayList<>(); }

					appInfo.getViewableGroupList().retainAll( groupNames );
					appInfo.getViewableUnitList().retainAll( unitNames );
					appInfo.getPublishableGroupList().retainAll( groupNames );
					appInfo.getPublishableUnitList().retainAll( unitNames );
					appInfo.getManageableGroupList().retainAll( groupNames );
					appInfo.getManageableUnitList().retainAll( unitNames );
					categoryInfo.getViewableGroupList().retainAll( groupNames );
					categoryInfo.getViewableUnitList().retainAll( unitNames );
					categoryInfo.getPublishableGroupList().retainAll( groupNames );
					categoryInfo.getPublishableUnitList().retainAll( unitNames );
					categoryInfo.getManageableGroupList().retainAll( groupNames );
					categoryInfo.getManageableUnitList().retainAll( unitNames );

					//?????????????????????????????????????????????????????????
					if( ListTools.isNotEmpty( appInfo.getManageableGroupList())
							|| ListTools.isNotEmpty( appInfo.getManageableUnitList())
							|| ListTools.contains( appInfo.getManageablePersonList(), personName )){
						woControl.setAllowManage( true );
						woControl.setAllowPublish( true );
						woControl.setAllowVisit( true );
					}

					//????????????????????????????????? ????????????????????????????????????????????????
					if( !woControl.getAllowManage() ){
						if( woControl.getAllowPublish() ){
							//???????????????????????????
							if( ListTools.isNotEmpty( categoryInfo.getManageableGroupList())
									|| ListTools.isNotEmpty( categoryInfo.getManageableUnitList())
									|| ListTools.contains( categoryInfo.getManageablePersonList(), personName )){
								woControl.setAllowManage( true );
								woControl.setAllowPublish( true );
								woControl.setAllowVisit( true );
							}
						}
					}

					//?????????????????????????????????????????????????????????
					if( !woControl.getAllowPublish() ){
						if( ListTools.isNotEmpty( appInfo.getPublishableGroupList())
								|| ListTools.isNotEmpty( appInfo.getPublishableUnitList())
								|| ListTools.contains( appInfo.getPublishablePersonList(), personName )){
							woControl.setAllowPublish( true );
							woControl.setAllowVisit( true );
						}
					}

					//???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
					if( appInfo.getAllPeopleView() || appInfo.getAnonymousAble()
							|| ListTools.isNotEmpty(appInfo.getViewableGroupList())
							|| ListTools.isNotEmpty( appInfo.getViewableUnitList())
							|| ListTools.contains( appInfo.getViewablePersonList(), personName )){
						//????????????????????????
						if( !woControl.getAllowManage() ){
							//???????????????????????????
							if( ListTools.isNotEmpty( categoryInfo.getManageableGroupList())
									|| ListTools.isNotEmpty( categoryInfo.getManageableUnitList())
									|| ListTools.contains( categoryInfo.getManageablePersonList(), personName )){
								woControl.setAllowManage( true );
								woControl.setAllowPublish( true );
								woControl.setAllowVisit( true );
							}
						}
						if( !woControl.getAllowPublish() ){
							//???????????????????????????
							if( categoryInfo.getAllPeoplePublish() ){
								woControl.setAllowPublish( true );
								woControl.setAllowVisit( true );
							}
						}
						if( !woControl.getAllowPublish() ){
							//???????????????????????????
							if( ListTools.isNotEmpty( categoryInfo.getPublishableGroupList())
									|| ListTools.isNotEmpty( categoryInfo.getPublishableUnitList())
									|| ListTools.contains( categoryInfo.getPublishablePersonList(), personName )){
								woControl.setAllowPublish( true );
								woControl.setAllowVisit( true );
							}
						}
						if( !woControl.getAllowVisit() ){
							//???????????????????????????
							if( categoryInfo.getAllPeopleView() || categoryInfo.getAnonymousAble() ){
								woControl.setAllowVisit( true );
							}
						}
						if( !woControl.getAllowVisit() ){
							//???????????????????????????
							if( ListTools.isNotEmpty( categoryInfo.getViewableGroupList())
									|| ListTools.isNotEmpty( categoryInfo.getViewableUnitList())
									|| ListTools.contains( categoryInfo.getViewablePersonList(), personName )){
								woControl.setAllowVisit( true );
							}
						}
					}
				}
				wo.setControl(woControl);
				CacheManager.put(cacheCategory, cacheKey, wo);
			}
		}
		result.setData(wo);
		return result;
	}
	

	public static class Wo {

		public static WrapCopier<Document, Wo> copier = WrapCopierFactory.wo(Document.class, Wo.class, null, JpaObject.FieldsInvisible);

		private WoControl control;

		public WoControl getControl() {
			return control;
		}

		public void setControl(WoControl control) {
			this.control = control;
		}
	}
	
	public static class WoControl extends GsonPropertyObject {

		@FieldDescribe("??????????????????.")
		private Boolean allowVisit = false;

		@FieldDescribe("????????????????????????.")
		private Boolean allowPublish = false;

		@FieldDescribe("????????????????????????????????????.")
		private Boolean allowManage = false;

		public Boolean getAllowVisit() {
			return allowVisit;
		}

		public void setAllowVisit(Boolean allowVisit) {
			this.allowVisit = allowVisit;
		}

		public Boolean getAllowPublish() {
			return allowPublish;
		}

		public void setAllowPublish(Boolean allowPublish) {
			this.allowPublish = allowPublish;
		}

		public Boolean getAllowManage() {
			return allowManage;
		}

		public void setAllowManage(Boolean allowManage) {
			this.allowManage = allowManage;
		}
	}
}