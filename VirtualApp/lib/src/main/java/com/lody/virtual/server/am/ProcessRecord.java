package com.lody.virtual.server.am;

import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IInterface;
import android.os.Process;

import com.lody.virtual.client.IVClient;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.ClientConfig;
import com.lody.virtual.server.bit64.V64BitHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class ProcessRecord extends Binder {

    public final ApplicationInfo info;
    final public String processName;
    final Set<String> pkgList = Collections.synchronizedSet(new HashSet<String>());
    public IVClient client;
    public IInterface appThread;
    public int pid;
    public int vuid;
    public int vpid;
    public boolean is64bit;
    public int callingVUid;
    public int userId;
    public ConditionVariable initLock = new ConditionVariable();

    public ProcessRecord(ApplicationInfo info, String processName, int vuid, int vpid, int callingVUid, boolean is64bit) {
        this.info = info;
        this.vuid = vuid;
        this.vpid = vpid;
        this.userId = VUserHandle.getUserId(vuid);
        this.callingVUid = callingVUid;
        this.processName = processName;
        this.is64bit = is64bit;
    }

    public int getCallingVUid() {
        return callingVUid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProcessRecord record = (ProcessRecord) o;
        return processName != null ? processName.equals(record.processName) : record.processName == null;
    }

    public String getProviderAuthority() {
        return StubManifest.getStubAuthority(vpid, is64bit);
    }

    public ClientConfig getClientConfig() {
        ClientConfig config = new ClientConfig();
        config.is64Bit = is64bit;
        config.vuid = vuid;
        config.vpid = vpid;
        config.packageName = info.packageName;
        config.processName = processName;
        config.token = this;
        return config;
    }

    public void kill() {
        if(pid > 0) {
            VActivityManagerService.get().beforeProcessKilled(this);
            if (is64bit) {
                V64BitHelper.forceStop64(pid);
            } else {
                try {
                    Process.killProcess(pid);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
