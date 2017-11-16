package com.liveroads.login;

import com.liveroads.login.ILoginServiceListener;
import com.liveroads.login.LoginTaskResult;

interface ILoginService {

    void addListener(ILoginServiceListener listener);

    void removeListener(ILoginServiceListener listener);

    boolean isTaskComplete(IBinder id);

    LoginTaskResult getTaskResult(IBinder id);

}
