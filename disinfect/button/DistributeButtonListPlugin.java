package plugins.disinfect.button;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.StyleCss;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态表单插件
 * 分配组织
 */
public class DistributeButtonListPlugin extends AbstractListPlugin implements Plugin {

    private static final String DYNAMIC_FORM_ID = "smk5_distributeorg";

    private static final String DISTRIBUTE_BUTTON = "smk5_distributebutton";

    private static final String USE_ORG = "smk5_entryentity1";

    private static final String DISINEFCT_PLAN = "smk5_plan";

    private static final String ORG_FIELD = "smk5_orgfield";


    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        if(DISTRIBUTE_BUTTON.equals(evt.getItemKey())){
            FormShowParameter showParameter = new FormShowParameter();
            ListSelectedRowCollection selectedRows = this.getSelectedRows();

            if(selectedRows.isEmpty()){
                this.getView().showMessage("请先选择一条记录");
                return ;
            }
            Object primaryKeyValue = selectedRows.get(0).getPrimaryKeyValue();
            Long parentId = (Long) primaryKeyValue;

            //获取所有方案的id
            DynamicObjectCollection query = QueryServiceHelper.query("smk5_plan", "id", new QFilter[0]);
            List<Long> ids = new ArrayList<>();
            for(DynamicObject data : query){
                Long id = data.getLong("id");
                ids.add(id);
            }


            //传参
            showParameter.setFormId(DYNAMIC_FORM_ID);
            showParameter.setCustomParam("parentId", parentId);
            showParameter.setCustomParam("ids", ids);

            showParameter.setCloseCallBack(new CloseCallBack(this,"show-kded_supaddnew"));

            showParameter.getOpenStyle().setShowType(ShowType.Modal);
            StyleCss inlineStyleCss = new StyleCss();
            inlineStyleCss.setHeight("600");
            inlineStyleCss.setWidth("800");
            showParameter.getOpenStyle().setInlineStyleCss(inlineStyleCss);
            this.getView().showForm(showParameter);

            //将选中的id放到缓存
            this.getView().getPageCache().put("parentId",String.valueOf(parentId));
        }

    }


    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        if (closedCallBackEvent.getActionId().equals("show-kded_supaddnew")) {
            //获取数据
            DynamicObjectCollection returnData = (DynamicObjectCollection)closedCallBackEvent.getReturnData();
//            1.根据id查询该条记录
//            2.清除原有数据
//            3.将子页面的数据赋值给父页面
//            4.存入数据库
//            5.刷新界面
            String id = this.getView().getPageCache().get("parentId");
            DynamicObject disinfectPlan = BusinessDataServiceHelper.loadSingle(id, DISINEFCT_PLAN);
            DynamicObjectCollection collection = disinfectPlan.getDynamicObjectCollection(USE_ORG);
            collection.clear();
            for(DynamicObject dynamicObject : returnData){
                if(dynamicObject.get(2) != null){
                    Object pkValue = dynamicObject.getDynamicObject(2).getPkValue();
                    DynamicObject object = collection.addNew();
                    object.set(ORG_FIELD,pkValue);
                }
            }
            SaveServiceHelper.save(new DynamicObject[]{disinfectPlan});
            this.getView().showSuccessNotification("组织分配成功");
            this.getView().updateView();

        }
    }
}