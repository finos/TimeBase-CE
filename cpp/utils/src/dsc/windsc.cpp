#include <jni.h>
#include <stdio.h>
#include "com_epam_deltix_util_os_WindowsNativeServiceControl.h"

#include "ServiceManager.h"

using namespace deltix;
using namespace deltix::dsc;

#define JAVA_IO_EXCEPTION                     "java/io/IOException"
#define JAVA_SERVICE_NOT_FOUND_EXCEPTION      "com/epam/deltix/util/os/ServiceControl$ServiceNotFoundException"

#define DSC_TYPE_VAL_OWN                      _T("own")
#define DSC_TYPE_VAL_SHARE                    _T("share")
#define DSC_TYPE_VAL_KERNEL                   _T("kernel")
#define DSC_TYPE_VAL_FILESYS                  _T("filesys")
#define DSC_TYPE_VAL_REC                      _T("rec")
#define DSC_TYPE_VAL_ADAPT                    _T("adapt")
#define DSC_TYPE_VAL_INTERACT                 _T("interact")

#define DSC_START_VAL_BOOT                    _T("boot")
#define DSC_START_VAL_SYSTEM                  _T("system")
#define DSC_START_VAL_AUTO                    _T("auto")
#define DSC_START_VAL_DEMAND                  _T("demand")
#define DSC_START_VAL_DISABLED                _T("disabled")

#define DSC_ERROR_VAL_NORMAL                  _T("normal")
#define DSC_ERROR_VAL_SEVERE                  _T("severe")
#define DSC_ERROR_VAL_CRITICAL                _T("cretical")
#define DSC_ERROR_VAL_IGNORE                  _T("ignore")

//jni helpers
const char * ToCStr(JNIEnv *env, jstring &jstr);
String ToStr(JNIEnv *env, jstring &jstr);
void FreeStr(JNIEnv *env, jstring &jstr, const char *cstr);
jint throwIOException(JNIEnv *env, const String &msg);
jint throwServiceNotFound(JNIEnv *env, const String &msg);
void throwServiceException(JNIEnv *env, int ret);

//string enums to long convertions
ulong StrToServiceType(const String &serviceType);
ulong StrToStartType(const String &startType);
ulong StrToErrorControl(const String &errorControl);
SC_ACTION_TYPE StrToFailureActionType(const String &failureAction);

//-------------------------------------------------------------------------------------

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    start
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_start
  (JNIEnv *env, jobject obj, jstring serviceId)
{
  String id = ToStr(env, serviceId);

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  ret = dsm.StartServiceNonBlock(id);
  if (DSC_ERROR_SERVICE_IS_RUNNING == ret)
    return;

  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

	return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    stop
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_stop
  (JNIEnv *env, jobject obj, jstring serviceId)
{
  String id = ToStr(env, serviceId);

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }
   
  ret = dsm.StopServiceNonBlock(id);
  if (DSC_SERVICE_ALREADY_STOPPED == ret ||
      DSC_ERROR_SERVICE_NOT_ACTIVE == ret)
    return;

  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

	return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    delete
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_delete
  (JNIEnv *env, jobject obj, jstring serviceId)
{
  String id = ToStr(env, serviceId);

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }
   
  ret = dsm.DeleteService(id);
  if (ret == DSC_ERROR_SERVICE_MARKED_FOR_DELETE)
    return;
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

	return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    addDependency
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_addDependency
  (JNIEnv *env, jobject obj, jstring serviceId, jstring dependId)
{
  String id = ToStr(env, serviceId);
  String dep = ToStr(env, dependId);

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  ret = dsm.AddDependency(id, dep);

  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    create
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ldeltix/util/os/ServiceControl/CreationParameters;)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_create
  (JNIEnv *env, jobject obj, jstring serviceId, jstring description, jstring binPath, jobject params)
{
  String serviceName = ToStr(env, serviceId);
  String desc = ToCStr(env, description);
  String binaryPathName = ToCStr(env, binPath);
  ulong access = SC_MANAGER_ALL_ACCESS;
  String displayName;
  ulong serviceType = StrToServiceType("default");
  ulong startType = StrToStartType("default");
  ulong errorControl = StrToErrorControl("default");
  String loadOrderGroup;
  String accountName;
  String accountPass;
  String depends;

  if (params != NULL) {
    jclass paramsClass = env->GetObjectClass(params);
    /* java class
    public static class CreationParameters {
      public String                  displayName;
      public Type                    type;
      public StartMode               startMode;
      public ErrorMode               errorMode;
      public String                  group;
      public String                  obj;
      public String                  password;
      public String []               dependencies;
    };
    */
    jmethodID toStringID;

    //public String         displayName;
    jfieldID displayNameID    = env->GetFieldID(paramsClass, "displayName", "Ljava/lang/String;");
    jstring jstrDisplName     = (jstring) env->GetObjectField(params, displayNameID);
    displayName               = ToStr(env, jstrDisplName);

    //public Type           type;
    jfieldID typeID           = env->GetFieldID(paramsClass, "type", "Lcom/epam/deltix/util/os/ServiceControl$Type;");
    jobject typeObj           = env->GetObjectField(params, typeID);
    if (typeObj != NULL) {
      jclass typeClass        = env->GetObjectClass(typeObj);
      toStringID              = env->GetMethodID(typeClass, "toString", "()Ljava/lang/String;");
      jstring jstrType        = (jstring) env->CallObjectMethod(typeObj, toStringID);
      serviceType             = StrToServiceType(ToStr(env, jstrType));
    }

    //public StartMode      startMode;
    jfieldID startModeID      = env->GetFieldID(paramsClass, "startMode", "Lcom/epam/deltix/util/os/ServiceControl$StartMode;");
    jobject startModeObj      = env->GetObjectField(params, startModeID);
    if (startModeObj != NULL) {
      jclass startModeClass   = env->GetObjectClass(startModeObj);
      toStringID              = env->GetMethodID(startModeClass, "toString", "()Ljava/lang/String;");
      jstring jstrStartMode   = (jstring) env->CallObjectMethod(startModeObj, toStringID);
      startType               = StrToStartType(ToStr(env, jstrStartMode));
    }

    //public ErrorMode      errorMode;
    jfieldID errorModeID      = env->GetFieldID(paramsClass, "errorMode", "Lcom/epam/deltix/util/os/ServiceControl$ErrorMode;");
    jobject errorModeObj      = env->GetObjectField(params, errorModeID);
    if (errorModeObj != NULL) {
      jclass errorModeClass   = env->GetObjectClass(errorModeObj);
      toStringID              = env->GetMethodID(errorModeClass, "toString", "()Ljava/lang/String;");
      jstring jstrErrorMode   = (jstring) env->CallObjectMethod(errorModeObj, toStringID);
      errorControl            = StrToErrorControl(ToStr(env, jstrErrorMode));
    }

    //public String         group;
    jfieldID groupID          = env->GetFieldID(paramsClass, "group", "Ljava/lang/String;");
    jstring jstrGroup         = (jstring) env->GetObjectField(params, groupID);
    loadOrderGroup            = ToStr(env, jstrGroup);

    //public String         obj;
    jfieldID objID            = env->GetFieldID(paramsClass, "obj", "Ljava/lang/String;");
    jstring jstrObj           = (jstring) env->GetObjectField(params, objID);
    accountName               = ToStr(env, jstrObj);

    //public String         password;
    jfieldID passwordID       = env->GetFieldID(paramsClass, "password", "Ljava/lang/String;");
    jstring jstrPassw         = (jstring) env->GetObjectField(params, passwordID);
    accountPass               = ToStr(env, jstrPassw);

    //public String []      dependencies;
    jfieldID dependenciesID   = env->GetFieldID(paramsClass, "dependencies", "[Ljava/lang/String;");
    jobject  dependenciesStr  = env->GetObjectField(params, dependenciesID);
    if (dependenciesStr != NULL) {
      jobjectArray *dependenciesAStr = reinterpret_cast<jobjectArray*>(&dependenciesStr);
      int size = env->GetArrayLength((jarray) *dependenciesAStr);
      for (int i = 0; i < size; ++i) {
        jstring jstr = (jstring) env->GetObjectArrayElement(*dependenciesAStr, i);
        String str = ToStr(env, jstr);
        if (i > 0) depends += "/";
        depends += str;
      }
    }
  }

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  ret = dsm.CreateService(serviceName, displayName, access, serviceType, startType, errorControl,
                          binaryPathName, loadOrderGroup, NULL, depends, accountName, accountPass);
  if (ret != DSC_ALL_OK) {
    throwIOException(env, DSMErrorToString(ret));
    return;
  }
  //set description
  ret = dsm.SetDescription(serviceName, desc);
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }
  
  return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    queryStatusName
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_queryStatusName
  (JNIEnv *env, jobject obj, jstring serviceId)
{
  String id = ToStr(env, serviceId);
   
  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return NULL;
  }

  String state;
  ret = dsm.QueryStrState(id, state);
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return NULL;
  }
    
	return env->NewStringUTF(state.c_str());
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    getExecutablePath
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_getExecutablePath
  (JNIEnv *env, jobject obj, jstring serviceId) //doesn't throw anything
{
  String id = ToStr(env, serviceId);
  String binaryPath;
  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    return NULL;
  }

  ret = dsm.QueryBinaryPath(id, binaryPath);
  if (ret != DSC_ALL_OK) {
    return NULL;
  }
    
	return env->NewStringUTF(binaryPath.c_str());
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    exists
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_exists
  (JNIEnv *env, jobject obj , jstring serviceId)
{
  String id = ToStr(env, serviceId);
  jboolean exists = JNI_FALSE;

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK)
    return exists;

  boolean res = dsm.ServiceExists(id);
  if (res)
    exists = JNI_TRUE;

	return exists;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    queryExitCode
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_queryExitCode
  (JNIEnv *env, jclass obj, jstring serviceId)
{
  String id = ToStr(env, serviceId);
  String exitCode;
  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return NULL;
  }

  ret = dsm.QueryStrWin32ExitCode(id, exitCode);
  if (ret != DSC_ALL_OK) {
    return NULL;
  }
    
	return env->NewStringUTF(exitCode.c_str());
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    queryServiceExitCode
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_queryServiceExitCode
  (JNIEnv *env, jclass obj, jstring serviceId)
{
  String id = ToStr(env, serviceId);
  String exitCode;
  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return NULL;
  }

  ret = dsm.QueryStrServiceExitCode(id, exitCode);
  if (ret != DSC_ALL_OK) {
    return NULL;
  }
    
	return env->NewStringUTF(exitCode.c_str());
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    startAndWait
 * Signature: (Ljava/lang/String;ZJ)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_startAndWait
  (JNIEnv *env, jobject obj, jstring serviceId, jboolean, jlong timeout)
{
  String id = ToStr(env, serviceId);

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  ret = dsm.StartService(id, timeout);
  if (DSC_ERROR_SERVICE_IS_RUNNING == ret)
    return;

  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

	return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    stopAndWait
 * Signature: (Ljava/lang/String;ZJ)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_stopAndWait
  (JNIEnv *env, jobject obj, jstring serviceId, jboolean, jlong timeout)
{
  String id = ToStr(env, serviceId);

  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  ret = dsm.StopService(id, timeout);
  if (DSC_SERVICE_ALREADY_STOPPED == ret ||
      DSC_ERROR_SERVICE_NOT_ACTIVE == ret)
    return;

  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

	return;
}

/*
 * Class:     com_epam_deltix_util_os_WindowsNativeServiceControl
 * Method:    setFailureAction
 * Signature: (Ljava/lang/String;Ldeltix/util/os/ServiceControl/FailureAction;ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_epam_deltix_util_os_WindowsNativeServiceControl_setFailureAction
  (JNIEnv *env, jobject obj, jstring serviceId, jobject action, jint delay, jstring cmd)
{
  String id = ToStr(env, serviceId);
  String command = ToStr(env, cmd);

  SC_ACTION_TYPE type = SC_ACTION_NONE;
  //FailureAction;
  jmethodID toStringID;
  if (action != NULL) {
    jclass typeClass        = env->GetObjectClass(action);
    toStringID              = env->GetMethodID(typeClass, "toString", "()Ljava/lang/String;");
    jstring jstrType        = (jstring) env->CallObjectMethod(action, toStringID);
    type                    = StrToFailureActionType(ToStr(env, jstrType));
  }
  
  ServiceManager dsm;
  int ret = dsm.Init();
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
    return;
  }

  ret = dsm.ChangeFailureActions(id, type, (int) delay, command);
  if (ret != DSC_ALL_OK) {
    throwServiceException(env, ret);
  }

	return;
}

//-------------------------------------------------------------------------------------

const char * ToCStr(JNIEnv *env, jstring &jstr) {
  if (jstr != NULL)
    return env->GetStringUTFChars(jstr, 0);
  else 
    return NULL;
}

String ToStr(JNIEnv *env, jstring &jstr) {
  String ret;
  if (jstr != NULL) {
    const char *cstr = ToCStr(env, jstr);
    if (cstr != NULL) {
      ret = cstr;
      FreeStr(env, jstr, cstr);
    }
  }
  return ret;
}

void FreeStr(JNIEnv *env, jstring &jstr, const char *cstr) {
  if (jstr != NULL)
    env->ReleaseStringUTFChars(jstr, cstr);
}

jint throwIOException(JNIEnv *env, const String &msg)
{
  jclass exception = env->FindClass(JAVA_IO_EXCEPTION);
  if (exception == NULL)
    return NULL;

  return env->ThrowNew(exception, msg.c_str());
}

jint throwServiceNotFound(JNIEnv *env, const String &msg) 
{
  jclass notFoundEx = env->FindClass(JAVA_SERVICE_NOT_FOUND_EXCEPTION);
  if (notFoundEx == NULL)
    return NULL;

  return env->ThrowNew(notFoundEx, msg.c_str());
}

void throwServiceException(JNIEnv *env, int ret) 
{
  if (ret == DSC_ERROR_SERVICE_DOES_NOT_EXIST)
    throwServiceNotFound(env, DSMErrorToString(ret));
  else
    throwIOException(env, DSMErrorToString(ret));
}

//-------------------------------------------------------------------------------------

ulong StrToServiceType(const String &serviceType)
{
  ulong type = SERVICE_WIN32_OWN_PROCESS;

  if (serviceType == DSC_TYPE_VAL_ADAPT)
    type = SERVICE_ADAPTER;
  else if (serviceType == DSC_TYPE_VAL_FILESYS)
    type = SERVICE_FILE_SYSTEM_DRIVER;
  else if (serviceType == DSC_TYPE_VAL_KERNEL)
    type = SERVICE_KERNEL_DRIVER;
  else if (serviceType == DSC_TYPE_VAL_SHARE)
    type = SERVICE_WIN32_SHARE_PROCESS;
  else if (serviceType == DSC_TYPE_VAL_REC)
    type = SERVICE_RECOGNIZER_DRIVER;
  else if (serviceType == DSC_TYPE_VAL_INTERACT)
    type = SERVICE_WIN32_OWN_PROCESS | 
           SERVICE_WIN32_SHARE_PROCESS;

  return type;
}

ulong StrToStartType(const String &startType)
{
  ulong type = SERVICE_DEMAND_START;

  if (startType == DSC_START_VAL_BOOT)
    type = SERVICE_BOOT_START;
  else if (startType == DSC_START_VAL_SYSTEM)
    type = SERVICE_SYSTEM_START;
  else if (startType == DSC_START_VAL_AUTO)
    type = SERVICE_AUTO_START;
  else if (startType == DSC_START_VAL_DISABLED)
    type = SERVICE_DISABLED;

  return type;
}

ulong StrToErrorControl(const String &errorControl)
{
  ulong error = SERVICE_ERROR_NORMAL;

  if (errorControl == DSC_ERROR_VAL_CRITICAL)
    error = SERVICE_ERROR_CRITICAL;
  else if (errorControl == DSC_ERROR_VAL_IGNORE)
    error = SERVICE_ERROR_IGNORE;
  else if (errorControl == DSC_ERROR_VAL_SEVERE)
    error = SERVICE_ERROR_SEVERE;

  return error;
}

SC_ACTION_TYPE StrToFailureActionType(const String &failureAction)
{
  if (failureAction == "reboot")
    return SC_ACTION_REBOOT;
  else if (failureAction == "restart")
    return SC_ACTION_RESTART;
  else if (failureAction == "run")
    return SC_ACTION_RUN_COMMAND;
  return SC_ACTION_NONE;
}