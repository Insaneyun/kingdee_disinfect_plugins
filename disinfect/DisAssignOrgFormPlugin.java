package plugins.disinfect;

import com.alibaba.dubbo.common.utils.StringUtils;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * 动态表单插件
 * 消毒方案
 */
public class DisAssignOrgFormPlugin extends AbstractFormPlugin implements Plugin, BeforeF7SelectListener {


    private static final String SUIT_ORG = "smk5_suitorg";
    //消毒方案-组织分录标识
    private static final String DIS_ORG = "smk5_entryentity1";

    private static final String ORG = "smk5_entryentity";
    //消毒方案单据体
    private static final String DISINFECT_PLAN = "smk5_plan";

    private static final String BUTTON_SUBMIT = "btnok";



    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        //监控按钮
        BasedataEdit basedataEdit = this.getView().getControl(SUIT_ORG);
        basedataEdit.addBeforeF7SelectListener(this);

        Button submitButton = this.getControl(BUTTON_SUBMIT);
        submitButton.addClickListener(this);
    }

    /**
     * 给页面赋值
     * @param e
     */
    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        FormShowParameter showParameter = this.getView().getFormShowParameter();
        Long Id = (Long) showParameter.getCustomParam("parentId");

        String parentId = String.valueOf(Id);

        if (StringUtils.isBlank(parentId)) {
            this.getView().showMessage("未获取到父页面ID");
            return;
        }

        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle(
                parentId,
                BusinessDataServiceHelper.newDynamicObject(DISINFECT_PLAN).getDynamicObjectType());


        // 获取 "bos_org" 字段，假设它是一个单一的 DynamicObject
        DynamicObjectCollection collection = dynamicObject.getDynamicObjectCollection(DIS_ORG);
        this.getModel().batchCreateNewEntryRow(ORG, collection.size());

        for(DynamicObject object : collection){
            Object pkValue = object.getDynamicObject(2).getPkValue();
            this.getModel().setValue(SUIT_ORG,pkValue);

        }
    }

    /**
     * 校验重复组织
     * @param beforeF7SelectEvent
     */
    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {

        List<Long> excludeIds = (List<Long>)this
                .getView().
                getFormShowParameter().
                getCustomParam("ids");

        QFilter qFilter1 = new QFilter("id", QCP.in, excludeIds);
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
                excludeIds.toArray(),
                BusinessDataServiceHelper.newDynamicObject(DISINFECT_PLAN).getDynamicObjectType());


        List<Long> ids = new ArrayList<>();
        for(DynamicObject dynamicObject : dynamicObjects){
            DynamicObjectCollection collection = dynamicObject.getDynamicObjectCollection(DIS_ORG);
            for(int i = 0 ; i < collection.size() ; i ++) {
                DynamicObject entity = collection.get(i);
                DynamicObject bosOrg = (DynamicObject) entity.get(2);
                Long masterid = (Long) bosOrg.get("masterid");
                ids.add(masterid);

            }
        }
        QFilter qFilter = new QFilter("id", QCP.not_in, ids);
        beforeF7SelectEvent.addCustomQFilter(qFilter);


    }

    /**
     * 回传数据给父页面
     * @param evt
     */
    @Override
    public void click(EventObject evt) {
        super.click(evt);

       if(BUTTON_SUBMIT.equals(((Control)evt.getSource()).getKey())){
           DynamicObjectCollection entryEntity = this.getModel().getEntryEntity(ORG);

           if(entryEntity.isEmpty()){
               this.getView().showMessage("添加信息");
           }

           this.getView().returnDataToParent(entryEntity);
           this.getView().close();
       }
    }


}

