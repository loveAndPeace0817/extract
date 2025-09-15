package com.demo.extract.zzq;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.zzq.dto.zzqdto;

import java.io.IOException;
import java.util.List;

public class loadZZQDate {

    public  static void  loadData(String path) throws IOException {
        ZZQDataLoader loaderNew = new ZZQDataLoader();
        List<zzqdto> zzqdtos = loaderNew.loadFromCsv("D:/data/章铮奇/gbpjpy_15min.csv");
        for (zzqdto z :zzqdtos){
            System.out.println(z.getDate());
        }
    }


    public static void main(String[] args) throws IOException {
        loadData("D:/data/章铮奇/gbpjpy_15min.csv");
    }
}
