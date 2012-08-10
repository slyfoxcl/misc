
package com.pekall.pctool.model;

import android.content.Context;

import com.example.tutorial.AddressBookProtos.AddressBook;
import com.example.tutorial.AddressBookProtos.Person;
import com.google.protobuf.ByteString;
import com.pekall.pctool.model.app.AppUtil;
import com.pekall.pctool.protos.AppInfoProtos.AppInfoPList;
import com.pekall.pctool.protos.MsgDefProtos.AppLocationType;
import com.pekall.pctool.protos.MsgDefProtos.AppRecord;
import com.pekall.pctool.protos.MsgDefProtos.AppType;
import com.pekall.pctool.protos.MsgDefProtos.CmdResponse;
import com.pekall.pctool.protos.MsgDefProtos.CmdType;
import com.pekall.pctool.protos.MsgDefProtos.MsgRecord;

public class FakeBusinessLogicFacade {

    private Context mContext;

    public FakeBusinessLogicFacade(Context context) {
        this.mContext = context;
    }

    public AddressBook getAddressBook() {
        AddressBook.Builder builder = AddressBook.newBuilder();
        builder.addPerson(getPerson("李雷", 1));
        builder.addPerson(getPerson("韩梅梅", 2));
        return builder.build();
    }

    private Person getPerson(String name, int id) {
        return Person.newBuilder().setName(name).setId(id).build();
    }
    
    public AppInfoPList getAppInfoPList() {
        return AppUtil.getAppInfoPList(mContext);
    }
    
    public CmdResponse queryAppRecordList() {
        CmdResponse.Builder response = CmdResponse.newBuilder();
        response.setType(CmdType.CMD_QUERY_APP);
        response.setResultCode(0);
        response.setResultMsg("OK");
        
        AppRecord.Builder app = AppRecord.newBuilder();
        
        app.setAppName("新浪微博");
        app.setType(AppType.USER);
        app.setLocation(AppLocationType.INNER);
        app.setPackageName("com.weibo");
        app.setVersionName("v2.0");
        app.setVersionCode(123456);
        app.setSize(2048);
        app.setAppIcon(ByteString.copyFrom(new byte[] {0, 1, 2, 3, 4, 5}));
        
        response.addAppRecord(app.build());
        
        app.clear();

        app.setAppName("腾讯微博");
        app.setType(AppType.USER);
        app.setLocation(AppLocationType.INNER);
        app.setPackageName("com.tencent.weibo");
        app.setVersionName("v2.3");
        app.setVersionCode(654321);
        app.setSize(1024);
        app.setAppIcon(ByteString.copyFrom(new byte[] {5, 4, 3, 2, 1, 0}));
        
        response.addAppRecord(app.build());
        
        return response.build();
    }
}
