package plugins.disinfect;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.ClientProperties;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态表单插件
 */
public class DisApplyBillFormPlugin extends AbstractFormPlugin implements Plugin {
    private static final String ID = "id";
    //组织信息表
    private static final String TABLE_ORG = "bos_adminorg";
    private static final String ORGID = "smk5_orgfield";
    private static final String COMPANYID = "smk5_company";

    private static final String BILLSTATUS = "billstatus";

    @Override
    public void afterCreateNewData(EventObject e) {
    //    this.getView().showMessage("hello");
     //   super.afterCreateNewData(e);
        //获取id
        long currUserId = RequestContext.get().getCurrUserId();
        //获取部门
        long orgId = UserServiceHelper.getUserMainOrgId(currUserId);
        //获取公司
     //   Map<String, Object> companyByOrg = OrgUnitServiceHelper.getSuperiorOrgs();

        QFilter qFilter = new QFilter(ID, QCP.equals, orgId);
        DynamicObjectCollection query1 = QueryServiceHelper
                .query(TABLE_ORG, "name,structure.viewparent", new QFilter[]{qFilter});

        if(!query1.isEmpty()){
            long companyId = query1.get(0).getLong("structure.viewparent");
            this.getModel().setValue(ORGID,orgId);
            this.getModel().setValue(COMPANYID,companyId);

        }
    }

    /**
     * 单据状态颜色变动
     * @param e
     */
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);

        // 获取单据状态值
        String billStatus = (String)this.getModel().getValue("billstatus");

        // 创建颜色映射表
        Map<String, String> statusColorMap = new HashMap<>();
        statusColorMap.put("A", "#3987ED"); // 保存-蓝色
        statusColorMap.put("B", "#FF991C"); // 已提交-橙色
        statusColorMap.put("C", "#701DF0"); // 审核中-紫色
        statusColorMap.put("D", "#1BA854"); // 已审核-绿色
        statusColorMap.put("E", "#16B0F1"); // 消毒中-青色
        statusColorMap.put("F", "#FB2323"); // 已完成消毒-红色
        statusColorMap.put("G", "#999999"); // 废弃-灰色

        // 设置字段颜色
        if(billStatus != null && statusColorMap.containsKey(billStatus)) {
            Map<String, Object> styleMap = new HashMap<>();
            styleMap.put(ClientProperties.ForeColor, statusColorMap.get(billStatus));
            this.getView().updateControlMetadata("billstatus", styleMap);
        }
    }

}