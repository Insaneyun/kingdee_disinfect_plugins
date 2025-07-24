package plugins.disinfect.report;

import kd.bos.algo.Algo;
import kd.bos.algo.DataSet;
import kd.bos.algo.DataType;
import kd.bos.algo.Field;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.algo.Row;
import kd.bos.algo.RowMeta;
import kd.bos.algo.RowMetaFactory;
import kd.bos.algo.input.CollectionInput;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.AbstractReportColumn;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportColumn;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 报表取数插件
 */
public class DisinfectRecordDataPlugin extends AbstractReportListDataPlugin implements Plugin {

    private static final String DISINFECT_RECORD = "smk5_sterilizebill";
    private static final String[] SELECT_FIELDS = {"smk5_countId","smk5_roomname","smk5_month","smk5_year_count"};
    private static final String DISINFECT_RECORD_STATUS = "billstatus";
    private static final String DISINFECT_RECORD_STATUS_FINISHED = "F";
    private static  List<String> REPORT_DATA = null;
    private static  List<DataType> DATALIST = null;
    private static final Map<String,String> FIELDSTOCAPTAINMAP = new HashMap<>();



    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        initalMap();
        ReportColumn reportColumnfirst = new ReportColumn();
        reportColumnfirst.setCaption(new LocaleString(FIELDSTOCAPTAINMAP.get(REPORT_DATA.get(0))));
        reportColumnfirst.setFieldKey(REPORT_DATA.get(0));
        reportColumnfirst.setFieldType(ReportColumn.TYPE_TEXT);
        columns.add(reportColumnfirst);

        for(int i = 1 ; i <REPORT_DATA.size(); i++ ){
            ReportColumn reportColumn = new ReportColumn();
            reportColumn.setCaption(new LocaleString(FIELDSTOCAPTAINMAP.get(REPORT_DATA.get(i))));
            reportColumn.setFieldKey(REPORT_DATA.get(i));

            reportColumn.setFieldType(ReportColumn.TYPE_INTEGER);
            /**
             * 不能这样写，这样的返回值是integer ，需要的返回值是Integer
             */
//            reportColumn.setFieldType(String.valueOf(DATALIST.get(i)));
            columns.add(reportColumn);
        }

        return columns;
    }




    /**
     * 1.获取数据，车间名称，count（id）,申请进入时间
      */
    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
         REPORT_DATA = new ArrayList<>();
         DATALIST = new ArrayList<>();

        QFilter statusQFilter = new QFilter(DISINFECT_RECORD_STATUS, QCP.equals, DISINFECT_RECORD_STATUS_FINISHED);
        DataSet dataSet = QueryServiceHelper.queryDataSet(
                this.getClass().getName(),
                DISINFECT_RECORD,
                "smk5_billnofield id ,smk5_datefield ,smk5_basedatafield.name "+ SELECT_FIELDS[1],
                new QFilter[]{statusQFilter},
                null);
      //  dataSet.print(true);
        DataSet groupData = dataSet.copy()
                .groupBy(new String[]{SELECT_FIELDS[1], "substr(smk5_datefield,5,2) " + SELECT_FIELDS[2]})
                .count(SELECT_FIELDS[0]).finish();


        //根据具体有几个月份动态增添列数
        DATALIST.add(DataType.StringType);
        REPORT_DATA.add("smk5_roomname");
        String tmpmonth = "XX";
        int columnLastIndex = 0;
        DataSet copy = groupData.copy();
        while(copy.hasNext()){
            Row next = copy.next();
            String mon = (String)next.get(SELECT_FIELDS[2]);
            if(!tmpmonth.equals(mon)){
                columnLastIndex ++;
                tmpmonth = mon;
                REPORT_DATA.add("smk5_"+mon);
                DATALIST.add(DataType.IntegerType);
            }
        }
        REPORT_DATA.add("smk5_year_count");
        DATALIST.add(DataType.IntegerType);
        DataType[] dataTypes = DATALIST.toArray(new DataType[0]);
        String[] fields = REPORT_DATA.toArray(new String[0]);
        Collection<Object[]> coll = new ArrayList<>();
        RowMeta rowMeta = RowMetaFactory.createRowMeta(fields, dataTypes);
        CollectionInput collectionInput = new CollectionInput(rowMeta, coll);
        DataSet resultDataSet = Algo.create(this.getClass().getName()).createDataSet(collectionInput);

        // 1. 收集所有出现过的月份（升序）
        Set<String> allMonths = new TreeSet<>();
        for (Row row : groupData.copy()) {
            String month = row.getString(SELECT_FIELDS[2]);
            if (month != null && !month.isEmpty()) {
                allMonths.add(month);
            }
        }
        // 2. 构建月份 => 列索引 map（列0是房间名）
        Map<String, Integer> monthIndexMap = new LinkedHashMap<>();
        int index = 1;
        for (String month : allMonths) {
            monthIndexMap.put(month, index++);
        }

// 3. 构建 room => rowData 映射
        Map<String, Object[]> roomDataMap = new LinkedHashMap<>();
        for (Row row : groupData.copy()) {
            String roomName = row.getString(SELECT_FIELDS[1]);
            Integer count = row.getInteger(SELECT_FIELDS[0]);
            String month = row.getString(SELECT_FIELDS[2]);

            Object[] tmpData = roomDataMap.computeIfAbsent(roomName, rn -> {
                Object[] arr = new Object[monthIndexMap.size() + 2];
                arr[0] = rn;
                return arr;
            });
            Integer colIndex = monthIndexMap.get(month);
            if (colIndex != null) {
                tmpData[colIndex] = count;
            }
        }

        coll.addAll(roomDataMap.values());
        //计算年总量
        DataSet yearTotalDataSet = dataSet.copy()
                .groupBy(new String[]{SELECT_FIELDS[1], "substr(smk5_datefield,0,4)"})
                .count(SELECT_FIELDS[3]).finish();

        String[] selectfields = editFields(allMonths);
        resultDataSet = resultDataSet.join(yearTotalDataSet).on(SELECT_FIELDS[1], SELECT_FIELDS[1])
                .select(selectfields, new String[]{SELECT_FIELDS[3]}).finish();
        //合计
        DataSet totalDataSet = resultDataSet.copy();
        GroupbyDataSet totalGroupDataSet = totalDataSet.groupBy(null);
        //totalGroupDataSet
        for(String month : allMonths){
            totalGroupDataSet.sum("smk5_"+month,"smk5_"+month+"_total");
        }
        totalGroupDataSet.sum(SELECT_FIELDS[3],"smk5_year_total");
        String[] strings = editFieldsAll(allMonths);
        totalDataSet = totalGroupDataSet.finish().select(strings);

        resultDataSet.print(true);
        totalDataSet.print(true);

        DataSet union = resultDataSet.union(totalDataSet);
        return union;
    }





    //获取合计字段
    private String[] editFieldsAll(Set<String> months){
        List<String> fields = new ArrayList<>();
        fields.add("'合计'" +" as "+SELECT_FIELDS[1]);
        for(String month : months){
            fields.add("smk5_"+month+"_total");
        }
        fields.add("smk5_year_total");
        return fields.toArray(new String[0]);
    }
    //获取字段
    private String[] editFields(Set<String> months){
        List<String> fields = new ArrayList<>();
        fields.add(SELECT_FIELDS[1]);
        for(String month : months){
            fields.add("smk5_"+month);
        }

        return fields.toArray(new String[0]);
    }

    private void initalMap(){
        FIELDSTOCAPTAINMAP.put("smk5_roomname","车间名称");
        FIELDSTOCAPTAINMAP.put("smk5_01","1月");
        FIELDSTOCAPTAINMAP.put("smk5_02","2月");
        FIELDSTOCAPTAINMAP.put("smk5_03","3月");
        FIELDSTOCAPTAINMAP.put("smk5_04","4月");
        FIELDSTOCAPTAINMAP.put("smk5_05","5月");
        FIELDSTOCAPTAINMAP.put("smk5_06","6月");
        FIELDSTOCAPTAINMAP.put("smk5_07","7月");
        FIELDSTOCAPTAINMAP.put("smk5_08","8月");
        FIELDSTOCAPTAINMAP.put("smk5_09","9月");
        FIELDSTOCAPTAINMAP.put("smk5_10","10月");
        FIELDSTOCAPTAINMAP.put("smk5_11","11月");
        FIELDSTOCAPTAINMAP.put("smk5_12","12月");
        FIELDSTOCAPTAINMAP.put("smk5_year_count","年数量");
    }
}