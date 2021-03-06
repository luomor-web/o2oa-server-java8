package com.x.program.center.jaxrs.module;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.connection.ActionResponse;
import com.x.base.core.project.connection.ConnectionAction;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.program.center.Business;

class ActionListCategory extends BaseAction {

	ActionResult<List<Wo>> execute(EffectivePerson effectivePerson) throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			Business business = new Business(emc);
			ActionResult<List<Wo>> result = new ActionResult<>();
			// if (BooleanUtils.isNotTrue(Config.collect().getEnable())) {
			// throw new ExceptionDisable();
			// }
			// if (!business.collectAccountNotEmpty()) {
			// throw new ExceptionCollectAccountEmpty();
			// }
			// if (BooleanUtils.isNotTrue(business.connectCollect())) {
			// throw new ExceptionConnectError();
			// }
			// if (BooleanUtils.isNotTrue(business.validateCollect())) {
			// throw new ExceptionValidateError();
			// }
			Req req = new Req();
			req.setName(Config.collect().getName());
			req.setPassword(Config.collect().getPassword());
			String url = Config.collect().url("/o2_collect_assemble/jaxrs/module/list/category");
			ActionResponse ar = ConnectionAction.post(url, null, req);
			List<Wo> wos = ar.getDataAsList(Wo.class);
			result.setData(wos);
			return result;
		}
	}

	public static class Req extends GsonPropertyObject {

		private String name;
		private String password;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

	public static class Wo extends GsonPropertyObject {

		@FieldDescribe("??????")
		private String category;

		@FieldDescribe("??????")
		private Long count;

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public Long getCount() {
			return count;
		}

		public void setCount(Long count) {
			this.count = count;
		}

	}

}