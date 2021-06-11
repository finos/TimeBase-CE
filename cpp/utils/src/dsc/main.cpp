#include "DSCCommandArgs.h"
#include "ServiceManager.h"

using namespace deltix;
using namespace deltix::dsc;

int DSCCreateService(ServiceManager &dsm, const DSCCommandArgs &args);
int DSCDeleteService(ServiceManager &dsm, const DSCCommandArgs &args);
int DSCStartService(ServiceManager &dsm, const DSCCommandArgs &args);
int DSCStopService(ServiceManager &dsm, const DSCCommandArgs &args);
int DSCQueryService(ServiceManager &dsm, const DSCCommandArgs &args, String &strRet);
int DSCConfigService(ServiceManager &dsm, const DSCCommandArgs &args);

ulong StrToServiceType(const String &serviceType);
ulong StrToStartType(const String &startType);
ulong StrToErrorControl(const String &errorControl);

int Main(int argc, Char *argv[])
{
  int ret = DSC_UNKNOWN_ERROR;
  String retStr;

  DSCCommandArgs cmd;
  if (cmd.Init(argc, argv) > 0) {

    //init service manager
    ServiceManager dsm;
    ret = dsm.Init();
    if (ret != DSC_ALL_OK) return ret;

    //process command
    String command = cmd.GetValue(DSC_COMMAND);

    if (command == DSC_COMMAND_CREATE) {
      ret = DSCCreateService(dsm, cmd);
    } else if (command == DSC_COMMAND_DELETE) {
      ret = DSCDeleteService(dsm, cmd);
    } else if (command == DSC_COMMAND_START) {
      ret = DSCStartService(dsm, cmd);
    } else if (command == DSC_COMMAND_STOP) {
      ret = DSCStopService(dsm, cmd);
    } else if (command == DSC_COMMAND_QUERY) {
      ret = DSCQueryService(dsm, cmd, retStr);
    } else if (command == DSC_COMMAND_CONFIG) {
      ret = DSCConfigService(dsm, cmd);
    }
  }

  COUT << retStr;
  COUT << ret;

  return ret;
}

int DSCCreateService(ServiceManager &dsm, const DSCCommandArgs &args) 
{
  String serviceName = args.GetValue(DSC_SERVICE_NAME);
  String displayName = args.GetValue(DSC_DISPNAME_ARG);
  ulong access = SC_MANAGER_ALL_ACCESS;
  ulong serviceType = StrToServiceType(args.GetValue(DSC_TYPE_ARG));
  ulong startType = StrToStartType(args.GetValue(DSC_START_ARG));
  ulong errorControl = StrToErrorControl(args.GetValue(DSC_ERROR_ARG));
  String binaryPathName = args.GetValue(DSC_BINPATH_ARG);
  String loadOrderGroup = args.GetValue(DSC_GROUP_ARG);
  //Specifies the names of services or groups that must start before this service. The names are separated by forward slashes (/).
  String depends = args.GetValue(DSC_DEPEND_ARG);
  String accountName = args.GetValue(DSC_OBJ_VAL_ACCNAME);
  String accountPass = args.GetValue(DSC_PASSWORD_ARG);

  return dsm.CreateService(serviceName, displayName, access, serviceType, startType, errorControl,
    binaryPathName, loadOrderGroup, NULL, depends, accountName, accountPass);
}

int DSCConfigService(ServiceManager &dsm, const DSCCommandArgs &args)
{
  String serviceName = args.GetValue(DSC_SERVICE_NAME);
  String displayName = args.GetValue(DSC_DISPNAME_ARG);
  ulong access = SC_MANAGER_ALL_ACCESS;
  ulong serviceType = args.IsExists(DSC_TYPE_ARG) ? StrToServiceType(args.GetValue(DSC_TYPE_ARG)) : SERVICE_NO_CHANGE;
  ulong startType = args.IsExists(DSC_START_ARG) ? StrToStartType(args.GetValue(DSC_START_ARG)) : SERVICE_NO_CHANGE;
  ulong errorControl = args.IsExists(DSC_ERROR_ARG) ? StrToErrorControl(args.GetValue(DSC_ERROR_ARG)) : SERVICE_NO_CHANGE;
  String binaryPathName = args.GetValue(DSC_BINPATH_ARG);
  String loadOrderGroup = args.GetValue(DSC_GROUP_ARG);
  //Specifies the names of services or groups that must start before this service. The names are separated by forward slashes (/).
  String depends = args.GetValue(DSC_DEPEND_ARG);
  String accountName = args.GetValue(DSC_OBJ_VAL_ACCNAME);
  String accountPass = args.GetValue(DSC_PASSWORD_ARG);

  int ret = DSC_ALL_OK;

  ret = dsm.ChangeConfig(serviceName, serviceType, startType, errorControl, binaryPathName,
    loadOrderGroup, NULL, depends, accountName, accountPass, displayName);
 
  return ret;
}

int DSCDeleteService(ServiceManager &dsm, const DSCCommandArgs &args) 
{
  int ret = DSC_ALL_OK;

  String serviceName = args.GetValue(DSC_SERVICE_NAME);

  ret = dsm.DeleteService(serviceName);

  return ret;  
}

int DSCStartService(ServiceManager &dsm, const DSCCommandArgs &args)
{
  int ret = DSC_ALL_OK;

  String serviceName = args.GetValue(DSC_SERVICE_NAME);

  ret = dsm.StartService(serviceName);

  return ret;
}

int DSCStopService(ServiceManager &dsm, const DSCCommandArgs &args)
{
  int ret = DSC_ALL_OK;

  String serviceName = args.GetValue(DSC_SERVICE_NAME);

  ret = dsm.StopService(serviceName);

  return ret;
}

int DSCQueryService(ServiceManager &dsm, const DSCCommandArgs &args, String &strRet)
{
  int ret = DSC_ALL_OK;

  String serviceName = args.GetValue(DSC_SERVICE_NAME);

  ret = dsm.QueryStrStatus(serviceName, strRet);

  return ret;
}

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