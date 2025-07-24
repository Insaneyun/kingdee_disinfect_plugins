package plugins.disinfect;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.container.Wizard;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.control.Steps;
import kd.bos.form.control.StepsOption;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.control.events.StepEvent;
import kd.bos.form.control.events.WizardStepsListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单据界面插件
 *  //向导控件
 */
public class DisinfectRecord extends AbstractBillPlugIn implements Plugin , WizardStepsListener {


    private static final String READY = "A";
    private static final String WORKING = "B";
    private static final String FINISHED = "C";

    private static final String DISINFECTING = "E";

    private static final String DISINFECT_APPLY = "smk5_staffsterilize";

    private static final String DISINFECT_RECORD = "smk5_sterilizebill";
     private static final String DISINFECT_RECORD_STATUS = "billstatus";
    private static final String DISINFECT_RECORD_WIZARD = "smk5_wizardap";
    private static final String DISINFECT_RECORD_FINISHBUTTON = "smk5_buttonfinish";
    private static final String DISINFECT_RECORD_PICTURE = "smk5_picturefield";
    private static final String DISINFECT_RECORD_APPLYID = "smk5_applyid";
    private static final String DISINFECT_RECORD_STEP = "smk5_entryentity1";
    private static final String DISINFECT_RECORD_STEP_STATUS = "smk5_billstatusfield1";
    private static final String DISINFECT_RECORD_STEP_PICTURE = "smk5_picturefield1";
    private static final Map<String,String> STEP_STATUS = new HashMap<>();


    @Override
    public void initialize() {
        super.initialize();
        STEP_STATUS.put("A","wait");
        STEP_STATUS.put("B","process");
        STEP_STATUS.put("C","finish");

    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        Button button = this.getView().getControl(DISINFECT_RECORD_FINISHBUTTON);
        button.addClickListener(this);
        Wizard wizard = this.getControl(DISINFECT_RECORD_WIZARD);
        wizard.addWizardStepsListener(this);
    }

    /**
     * \
     * 创建向导
     *
     * @param e
     */
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        Wizard wizard = this.getControl(DISINFECT_RECORD_WIZARD);
        List<StepsOption> stepsOptions = wizard.getStepsOptions();
        stepsOptions.clear();

        DynamicObjectCollection steps = this.getModel().getEntryEntity(DISINFECT_RECORD_STEP);
        int count = 0;
        boolean finishFlag = true;
        for (DynamicObject step : steps) {
            String description = (String)step.get("smk5_basedatafield3.name.zh_CN");
            String title = (String)step.get("smk5_basedatafield2.name.zh_CN");
            String status = (String)step.get("smk5_billstatusfield1");
            if(WORKING.equals(status)){
                finishFlag = false;
                Map<String, Object> currentStepMap = new HashMap<>();
                currentStepMap.put("currentStep", count);
                currentStepMap.put("currentStatus", Steps.FINISH);
                wizard.setWizardCurrentStep(currentStepMap);
            }
            StepsOption stepsOption = new StepsOption();
            stepsOption.setTitle(new LocaleString(title));
            stepsOption.setDescription(new LocaleString(description));
            stepsOption.setStatus(STEP_STATUS.get(status));
            stepsOptions.add(stepsOption);
            count ++;
        }
        wizard.setWizardStepsOptions(stepsOptions);
        if(finishFlag){
            Map<String, Object> currentStepMap = new HashMap<>();
            currentStepMap.put("currentStep", count - 1);
            currentStepMap.put("currentStatus", Steps.FINISH);
            wizard.setWizardCurrentStep(currentStepMap);
            this.getModel().setValue(DISINFECT_RECORD_STATUS,"F");
            Boolean success = UpdateDisinfectRecord();
            if(!success){
                this.getView().showMessage("人员申请单状态更新失败");
            }
        }


        String status = (String) this.getModel().getValue(DISINFECT_RECORD_STATUS);
        if(!DISINFECTING.equals(status)){
            this.getView().setVisible(false,DISINFECT_RECORD_FINISHBUTTON);
        }

        // this.getModel().setValue(DISINFECT_RECORD_PICTURE,steps.get(steps.size()-1).get(DISINFECT_RECORD_STEP_PICTURE));
    }

    @Override
    public void click(EventObject evt) {

        super.click(evt);
        Object value = this.getModel().getValue(DISINFECT_RECORD_PICTURE);
        DynamicObjectCollection steps = this.getModel().getEntryEntity(DISINFECT_RECORD_STEP);
        int countWORKING = 0;//用于限制一个工作状态
        for (DynamicObject step : steps) {
            String status = (String) step.get(DISINFECT_RECORD_STEP_STATUS);
            if (WORKING.equals(status)) {
                step.set(DISINFECT_RECORD_STEP_STATUS, FINISHED);
                step.set(DISINFECT_RECORD_STEP_PICTURE, value);
            } else if (READY.equals(status) && countWORKING == 0) {
                step.set(DISINFECT_RECORD_STEP_STATUS, WORKING);
                countWORKING++;
            }
        }
        this.getModel().setValue(DISINFECT_RECORD_PICTURE,null);
        this.getView().updateView();

    }

    private  Boolean UpdateDisinfectRecord(){
        String applyId = (String)this.getModel().getValue(DISINFECT_RECORD_APPLYID);
        if(applyId.isEmpty()){
            return true;
        }
        DynamicObject apply = BusinessDataServiceHelper.loadSingle(applyId, DISINFECT_APPLY);
        apply.set("billstatus","F");
        OperationResult operationResult = SaveServiceHelper.saveOperate(DISINFECT_APPLY, new DynamicObject[]{apply}, OperateOption.create());
        return operationResult.isSuccess();
    }

    @Override
    public void update(StepEvent stepEvent) {
        Wizard wizard = this.getControl(DISINFECT_RECORD_WIZARD);
        int index = stepEvent.getValue();
        DynamicObjectCollection steps = this.getModel().getEntryEntity(DISINFECT_RECORD_STEP);
//        this.getModel().setValue(DISINFECT_RECORD_PICTURE,steps.get(index).get(DISINFECT_RECORD_STEP_PICTURE));
        this.getView().getModel().setValue(DISINFECT_RECORD_PICTURE,steps.get(index).get(DISINFECT_RECORD_STEP_PICTURE));
        this.getView().updateView();
    }
}

