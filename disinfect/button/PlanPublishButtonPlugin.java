package plugins.disinfect.button;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.CloneUtils;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.interaction.InteractionContext;
import kd.bos.entity.operate.interaction.KDInteractionException;
import kd.bos.entity.operate.result.OperateErrorInfo;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.validate.ErrorLevel;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;

/**
 * 单据操作插件
 * 发布按钮
 */
public class PlanPublishButtonPlugin extends AbstractOperationServicePlugIn implements Plugin {


    private static final String PUBLISH_BUTTON = "smk5_publish";
    //消毒记录单
    private static final String DISINFECT_RECORD = "smk5_sterilizebill";
    //消毒记录单中的消毒方案
    private static final String DISINFECT_PLAN = "smk5_basedatafield1";
    //消毒方案主单据体
    private static final String MAIN_DISINFECT_PLAN = "smk5_plan";
    //消毒方案中的版本
    private static final String VERISON = "smk5_version_status";
//   消毒方案中的版本号
    private static final String VERSION_NO = "smk5_version_no";

    private static final String HISTORY_VERSION = "C";

    private static final String LATEST_VERSION = "D";




    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
            DynamicObject[] entities = e.getDataEntities();
            if(entities.length > 0){
                for(DynamicObject object :entities){
                    //检测当前版本
                    String version =  (String)object.get(VERISON);
                    if(!version.equals(LATEST_VERSION) ){
                        InteractionContext interactionContext = new InteractionContext();
                        OperateErrorInfo errorInfo = new OperateErrorInfo();
                        errorInfo.setMessage("当前版本为历史版本，请选择最新版本");
                        errorInfo.setLevel(ErrorLevel.Error);
                        interactionContext.addOperateInfo(errorInfo);
                        throw new KDInteractionException("custom.interaction.key",interactionContext);

                    }


                    Long pkValue = (Long)object.getPkValue();
                    QFilter idQFilter = new QFilter(DISINFECT_PLAN+".id", QCP.equals, pkValue);
                    DynamicObjectCollection query = QueryServiceHelper.query(
                            DISINFECT_RECORD,
                            "id",
                            new QFilter[]{idQFilter});


                    if(!query.isEmpty()){
                        DynamicObject newObj = (DynamicObject) new CloneUtils(false,true).clone(object);


                        object.set(VERISON, HISTORY_VERSION);
                        int o = (Integer)object.get(VERSION_NO);
                        newObj.set(VERSION_NO,o + 1);

                        SaveServiceHelper.saveOperate(MAIN_DISINFECT_PLAN,new DynamicObject[]{object,newObj},OperateOption.create());
                    }else{
                        SaveServiceHelper.saveOperate(MAIN_DISINFECT_PLAN,new DynamicObject[]{object}, OperateOption.create());

                    }
                }
            }


    }
}