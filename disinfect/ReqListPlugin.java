package plugins.disinfect;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.validate.ValidateResultCollection;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.isc.util.dt.D;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.DeleteServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.org.OrgServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Map;

/**
 * 标准单据列表插件
 */
public class ReqListPlugin extends AbstractListPlugin implements Plugin {
    @Override
    public void registerListener(EventObject e) {
        //单据列表的工具栏不用注册监听，父类已经注册过了
        super.registerListener(e);
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        String itemKey = evt.getItemKey();
        switch (itemKey){
            case "kdec_new"://新增
                addNewDemo();
                break;
            case "kdec_query"://仅查询
                queryDemo();
                break;
            case "kdec_eidt"://编辑：查询&修改&保存
                editDemo();
                break;
            case "kdec_delete"://删除
                deleteDemo();
                break;
            default:
                break;
        }
        super.itemClick(evt);
    }


    private void addNewDemo() {
        //根据采购申请单的表单标识创建一条空的数据包，kdec_reqbill是表单标识
        DynamicObject doj = BusinessDataServiceHelper.newDynamicObject("kdec_reqbill");
        doj.set("billstatus","A");
        doj.set("kdec_notes","addnewdemo备注");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle(2251038262850224128L, "bos_billtype");
        doj.set("kdec_billtypefield",dynamicObject.getPkValue());
        doj.set("kdec_requser", UserServiceHelper.getCurrentUserId());
        //新增两行分录数据，kdec_reqentryentity是单据体的标识
        DynamicObjectCollection entrys = doj.getDynamicObjectCollection("kdec_reqentryentity");
        //新增一条空的分录行：
        DynamicObject entry = entrys.addNew();
        entry.set("kdec_qty",10);//给申请数量字段赋值为10
        //参数：表单标识、被保存数据包、;操作参数
        OperateOption operateOption = OperateOption.create();
//        operateOption.setVariableValue("","");
        OperationResult result = SaveServiceHelper.saveOperate("kdec_reqbill", new DynamicObject[]{doj},
                operateOption) ;
//        Object[] save = SaveServiceHelper.save(new DynamicObject[]{doj});
        if (result.isSuccess()){
            this.getView().showSuccessNotification("新增数据成功");
            this.getView().invokeOperation("refresh");//调用表单的刷新操作
        }else {
            this.getView().showErrorNotification("保存失败");
        }
    }
    private void queryDemo() {
        //QFilter类似sql语句里的where条件
        QFilter billnoQfilter = new QFilter("billno", QCP.equals, "REQ-20250715-0009");
        DynamicObjectCollection reqObj = QueryServiceHelper.query("kdec_reqbill", "id,billno,kdec_requser,kdec_reqentryentity.kdec_qty", new QFilter[]{billnoQfilter});
        for (DynamicObject obj:reqObj) {
            Object qty = obj.get("kdec_reqentryentity.kdec_qty");
            Object kdecRequser = obj.get("kdec_requser");
        }
        this.getView().showMessage("编号为REQ-20250715-0009的数据查出来的数据行："+String.valueOf(reqObj.size()));
    }
    private void editDemo() {
        BillList billlsitap = getView().getControl("billlistap");//获取列表选中行数据，billlistap是所有标准单据列表控件的固定标识
        ListSelectedRowCollection selectedRows = billlsitap.getSelectedRows();
        ArrayList<Object> ids = new ArrayList<>();
        String entityName="kdec_reqbill";
        for (ListSelectedRow row:selectedRows){
            Object primaryKeyValue = row.getPrimaryKeyValue();
            ids.add(primaryKeyValue);

        }
        QFilter idQFilter = new QFilter("id", QCP.in, ids);

        DynamicObject userObj = BusinessDataServiceHelper.loadSingle("bos_user", "id,number",
                new QFilter[]{new QFilter("name", QCP.equals, "张三")});
        //对照OQL语法 select kdec_reqentryentity.kdec_qty,kdec_reqentryentity.kdec_materiel from kdec_reqbill where id in ids
        DynamicObject[] dojs = BusinessDataServiceHelper.load("kdec_reqbill", "id,billno,kdec_requser," +
                "kdec_reqentryentity.kdec_qty,kdec_reqentryentity.kdec_materiel", new QFilter[]{idQFilter});
        for (DynamicObject doj:dojs){
            doj.set("kdec_requser",userObj);//复杂类型字段赋值，赋值对象要是结构化的数据或者id
            DynamicObjectCollection entrys = doj.getDynamicObjectCollection("kdec_reqentryentity");
            for(DynamicObject entry:entrys){
                int qty = entry.getInt("kdec_qty");//申请数量的值
                entry.set("kdec_qty",qty+10);//把申请数量在原来的基础上加10
            }
        }
        OperationResult result = SaveServiceHelper.saveOperate("save", "kdec_reqbill", dojs, OperateOption.create());
        if (result.isSuccess()){
            this.getView().updateView();
            this.getView().showSuccessNotification("编辑数据成功");
        }
    }
    private void deleteDemo() {
        //获取列表选中行数据，billlistap是所有标准单据列表控件的固定标识
        BillList billlsitap = getView().getControl("billlistap");
        ListSelectedRowCollection selectedRows = billlsitap.getSelectedRows();
        ArrayList<Object> ids = new ArrayList<>();
        String entityName="kdec_reqbill";
        for (ListSelectedRow row:selectedRows){
;            Object primaryKeyValue = row.getPrimaryKeyValue();
            ids.add(primaryKeyValue);

        }
        QFilter idQFilter = new QFilter("id", QCP.in, ids);
        DeleteServiceHelper deleteServiceHelper = new DeleteServiceHelper();
        OperationResult result = deleteServiceHelper.deleteOperate("kdec_reqbill", ids.toArray(new Object[ids.size()]));
//        DeleteServiceHelper.delete("kdec_reqbill",new QFilter[]{idQFilter});
        if (result.isSuccess()) {
            this.getView().updateView();
            this.getView().showSuccessNotification("删除数据成功");
        }else{
            ValidateResultCollection validateResult = result.getValidateResult();
            String message = validateResult.getMessage();
            this.getView().showErrorNotification("删除失败，原因"+message);
        }
//        if (result.isSuccess()){
//            this.getView().showSuccessNotification("删除成功");
//        }
    }
}