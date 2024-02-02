package com.yupi.springbootinit.utils;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Excel 相关工具类
 */
@Slf4j
public class ExcelUtils {

    /**
     * excel 转 csv
     *
     * @param excel excel文件
     * @return
     */
    public static String excelToCsv(MultipartFile excel) {
        // 判断文件非空
        if (ObjectUtils.isEmpty(excel))
            return null;
        // 合法的文件后缀
        final List<String> VALID_SUFFIX = Arrays.asList("xlsx", "xls");

        // 判断文件类型
        String filename = Optional.ofNullable(excel.getOriginalFilename()).orElse("");
        String fileType = FileUtil.getSuffix(filename);
        // 不是合法后缀 返回空
        if (!VALID_SUFFIX.contains(fileType))
            return null;

        // 读取excel信息
        List<Map<Integer, String>> dataList;
        try {
            dataList = EasyExcel.read(excel.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("error on transferring excel to csv");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "excel转换异常");
        }
        // 没有数据 返回空
        if (dataList.size() == 0)
            return null;
        // 获取表头字符串
        String headerStr = StringUtils.join(dataList.get(0).values()
                // 去除空字符串表头
                .stream().filter(StringUtils::isNotBlank).collect(Collectors.toList()), ",") + "\n";
        // 将表头字符串和行数据字符串进行拼接
        StringBuilder dataStr = new StringBuilder(headerStr);
        for (int i = 1; i < dataList.size(); i++) {
            dataStr.append(StringUtils.join(dataList.get(i).values()
                    .stream().filter(StringUtils::isNotBlank).collect(Collectors.toList()), ",")).append("\n");
        }
        return String.valueOf(dataStr);
    }
}