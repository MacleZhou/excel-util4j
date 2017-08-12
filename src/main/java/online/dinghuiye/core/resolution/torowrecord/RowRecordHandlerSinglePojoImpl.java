package online.dinghuiye.core.resolution.torowrecord;

import online.dinghuiye.api.entity.ResultStatus;
import online.dinghuiye.api.entity.RowRecord;
import online.dinghuiye.api.entity.RowRecordHandleResult;
import online.dinghuiye.api.resolution.torowrecord.RowRecordHandler;
import online.dinghuiye.core.annotation.excel.Transient;
import online.dinghuiye.core.resolution.convert.ConvertKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * <p>单层关联pojo解析</p>
 *
 * <p>解析的pojo不能包含属性为自定义类的pojo，否则无法解析</p>
 * <p>使用一对一关联pojo导入参见{@link RowRecordHandlerImpl}</p>
 *
 * @author Strangeen
 * on 2017/6/27
 */
public class RowRecordHandlerSinglePojoImpl implements RowRecordHandler {

    private static final Logger logger = LoggerFactory.getLogger(RowRecordHandlerSinglePojoImpl.class);

    @Override
    public boolean handle(List<RowRecord> rowRecordList, Class<?>... pojos) {

        boolean allSuccess = true;
        for (RowRecord rowRecord : rowRecordList) {
            if (!handle(rowRecord, pojos)) allSuccess = false;
        }
        return allSuccess;
    }

    @Override
    public boolean handle(RowRecord rowRecord, Class<?>[] pojos) {
        return pojoHandle(rowRecord, pojos);
    }


    @Override
    public RowRecord rowRecordCreate(Map<String, Object> excelRowData, Integer row) {
        return RowRecordKit.createRowRecord(excelRowData, row);
    }

    /**
     * 装载pojo数组对象
     *
     * @param rowRecord {@link RowRecord}对象
     * @param pojos pojo类数组
     * @return true - RowRecord解析成功{@link ResultStatus#SUCCESS}
     *         false - RowRecord解析失败{@link ResultStatus}
     */
    private boolean pojoHandle(RowRecord rowRecord, Class<?>... pojos) {

        if (pojos.length <= 0) throw new RuntimeException("pojos未定义");
        boolean success = true;
        for (Class<?> pojo : pojos) {
            success = pojoHandle(rowRecord, pojo);
        }
        return success;
    }

    /**
     * 装载单个pojo
     *
     * @param rowRecord {@link RowRecord}
     * @param pojo pojo类
     * @return true - RowRecord解析成功{@link ResultStatus#SUCCESS}
     *         false - RowRecord解析失败{@link ResultStatus}
     */
    private boolean pojoHandle(RowRecord rowRecord, Class<?> pojo) {
        try {
            Object pojoObj = pojo.newInstance();
            rowRecord.set(pojo, pojoObj);

            Field[] fields = pojo.getDeclaredFields();
            for (Field field : fields) {

                Transient transientAnno = field.getAnnotation(Transient.class);
                if (transientAnno != null) continue;

                // 获取属性对应的excel表头名称
                String sheetTitleName = RowRecordKit.getSheetTitleNameByFieldName(field);

                // 按照pojo属性转换
                Object excelValue = rowRecord.get(sheetTitleName);
                Object fieldValue = ConvertKit.convert(excelValue, field, rowRecord.getExcelRecordMap());
                field.setAccessible(true);
                field.set(pojoObj, fieldValue);
            }
            rowRecord.getResult().setResult(ResultStatus.SUCCESS).setMsg(null);
            return true;

        } catch (Exception e) {
            logger.warn("pojo对象装载出错", e);
            rowRecord.getResult().setResult(ResultStatus.FAIL).setMsg("解析错误");
            return false;
        }
    }

}
