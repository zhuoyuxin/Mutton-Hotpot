package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.TableInfo;
import com.tongguo.mapper.DiningSessionMapper;
import com.tongguo.mapper.TableInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class TableService {

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private DiningSessionMapper diningSessionMapper;

    public List<TableInfo> list() {
        return tableInfoMapper.selectList(null);
    }

    public void add(TableInfo tableInfo) {
        tableInfoMapper.insert(tableInfo);
    }

    public void update(TableInfo tableInfo) {
        if (tableInfo == null || tableInfo.getId() == null) {
            throw new IllegalArgumentException("桌台ID不能为空");
        }
        TableInfo existing = tableInfoMapper.selectById(tableInfo.getId());
        if (existing == null) {
            throw new IllegalArgumentException("桌台不存在");
        }
        if (tableInfo.getName() != null) {
            existing.setName(tableInfo.getName());
        }
        if (tableInfo.getArea() != null) {
            existing.setArea(tableInfo.getArea());
        }
        existing.setStatus(hasActiveSession(existing.getId()) ? 1 : 0);
        tableInfoMapper.updateById(existing);
    }

    public void delete(Integer id) {
        Long count = diningSessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, id)
                        .eq(DiningSession::getStatus, 0)
        );
        if (count > 0) {
            throw new IllegalArgumentException("该桌台存在进行中的就餐会话，无法删除");
        }
        tableInfoMapper.deleteById(id);
    }

    public String generateQRCode(Integer id, String baseUrl) throws WriterException, IOException {
        TableInfo table = tableInfoMapper.selectById(id);
        if (table == null) throw new IllegalArgumentException("桌台不存在");

        String content = baseUrl + "/c/login/" + id;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private boolean hasActiveSession(Integer tableId) {
        return diningSessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        ) > 0;
    }
}
