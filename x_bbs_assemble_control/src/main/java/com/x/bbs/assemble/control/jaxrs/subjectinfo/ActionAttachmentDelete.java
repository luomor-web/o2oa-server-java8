package com.x.bbs.assemble.control.jaxrs.subjectinfo;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.annotation.CheckPersistType;
import com.x.base.core.entity.annotation.CheckRemoveType;
import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.config.StorageMapping;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.bbs.assemble.control.ThisApplication;
import com.x.bbs.assemble.control.jaxrs.subjectinfo.exception.ExceptionAttachmentIdEmpty;
import com.x.bbs.assemble.control.jaxrs.subjectinfo.exception.ExceptionAttachmentNotExists;
import com.x.bbs.assemble.control.jaxrs.subjectinfo.exception.ExceptionSubjectInfoProcess;
import com.x.bbs.assemble.control.jaxrs.subjectinfo.exception.ExceptionSubjectQueryById;
import com.x.bbs.entity.BBSSubjectAttachment;
import com.x.bbs.entity.BBSSubjectInfo;

public class ActionAttachmentDelete extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionAttachmentDelete.class);

	protected ActionResult<Wo> execute(HttpServletRequest request, EffectivePerson effectivePerson, String id) throws Exception {
		ActionResult<Wo> result = new ActionResult<>();
		BBSSubjectAttachment subjectAttachment = null;
		BBSSubjectInfo subjectInfo = null;
		StorageMapping mapping = null;
		Wo wo = new Wo();
		boolean check = true;
		if (id == null || id.isEmpty()) {
			check = false;
			Exception exception = new ExceptionAttachmentIdEmpty();
			result.error(exception);
			logger.error(exception, effectivePerson, request, null);
		}
		if (check) {
			try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
				subjectAttachment = emc.find(id, BBSSubjectAttachment.class);
				if (null == subjectAttachment) {
					check = false;
					Exception exception = new ExceptionAttachmentNotExists(id);
					result.error(exception);
					logger.error(exception, effectivePerson, request, null);
				}
			} catch (Exception e) {
				check = false;
				Exception exception = new ExceptionSubjectInfoProcess(e, "????????????ID?????????????????????????????????.ID:" + id);
				result.error(exception);
				logger.error(exception, effectivePerson, request, null);
			}
		}
		if (check) {
			try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
				subjectInfo = emc.find(subjectAttachment.getSubjectId(), BBSSubjectInfo.class);
				if (null == subjectInfo) {
					logger.warn("subjectInfo{id:" + subjectAttachment.getSubjectId() + "} is not exists, anyone can delete the attachments.");
				}
			} catch (Exception e) {
				check = false;
				Exception exception = new ExceptionSubjectQueryById(e, subjectAttachment.getSubjectId());
				result.error(exception);
				logger.error(exception, effectivePerson, request, null);
			}
		}
		if (check) {
			if (subjectAttachment != null) {
				try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
					mapping = ThisApplication.context().storageMappings().get(BBSSubjectAttachment.class, subjectAttachment.getStorage());
					// ?????????????????????
					subjectAttachment.deleteContent(mapping);
					// ??????????????????????????????
					subjectAttachment = emc.find(id, BBSSubjectAttachment.class);
					subjectInfo = emc.find(subjectAttachment.getSubjectId(), BBSSubjectInfo.class);
					emc.beginTransaction(BBSSubjectAttachment.class);
					emc.beginTransaction(BBSSubjectInfo.class);
					if (subjectInfo != null && subjectInfo.getAttachmentList() != null) {
						subjectInfo.getAttachmentList().remove(subjectAttachment.getId());
						emc.check(subjectInfo, CheckPersistType.all);
					}
					emc.remove(subjectAttachment, CheckRemoveType.all);							
					emc.commit();
					
					wo.setId(id);
					ApplicationCache.notify( BBSSubjectInfo.class );
				} catch (Exception e) {
					check = false;
					Exception exception = new ExceptionSubjectInfoProcess(e, "????????????ID?????????????????????????????????.ID:" + subjectAttachment.getSubjectId());
					result.error(exception);
					logger.error(exception, effectivePerson, request, null);
				}
			}
		}
		result.setData(wo);
		return result;
	}

	public static class Wo extends BBSSubjectAttachment {

		private static final long serialVersionUID = -5076990764713538973L;

		public static List<String> Excludes = new ArrayList<String>();
	}

}