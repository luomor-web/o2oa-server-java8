package com.x.processplatform.assemble.surface.jaxrs.taskcompleted;

import java.util.List;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.exception.ExceptionEntityNotExist;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.EqualsTerms;
import com.x.base.core.project.jaxrs.NotEqualsTerms;
import com.x.processplatform.assemble.surface.Business;
import com.x.processplatform.core.entity.content.TaskCompleted;
import com.x.processplatform.core.entity.element.Process;

class ActionListPrevWithProcess extends BaseAction {

	ActionResult<List<Wo>> execute(EffectivePerson effectivePerson, String id, Integer count, String processFlag)
			throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			Business business = new Business(emc);
			Process process = business.process().pick(processFlag);
			if (null == process) {
				throw new ExceptionEntityNotExist(processFlag, Process.class);
			}
			EqualsTerms equals = new EqualsTerms();
			equals.put(TaskCompleted.person_FIELDNAME, effectivePerson.getDistinguishedName());
			equals.put(TaskCompleted.process_FIELDNAME, process.getId());
			NotEqualsTerms notEquals = new NotEqualsTerms();
			notEquals.put(TaskCompleted.latest_FIELDNAME, false);
			ActionResult<List<Wo>> result = this.standardListPrev(Wo.copier, id, count,
					TaskCompleted.sequence_FIELDNAME, equals, notEquals, null, null, null, null, null, null, true,
					DESC);
			return result;
		}
	}

	public static class Wo extends TaskCompleted {

		private static final long serialVersionUID = -355092481236046649L;

		static WrapCopier<TaskCompleted, Wo> copier = WrapCopierFactory.wo(TaskCompleted.class, Wo.class, null,
				JpaObject.FieldsInvisible);

		@FieldDescribe("?????????")
		private Long rank;

		public Long getRank() {
			return rank;
		}

		public void setRank(Long rank) {
			this.rank = rank;
		}

	}
}
