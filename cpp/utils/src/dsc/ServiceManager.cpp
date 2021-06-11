#include "ServiceManager.h"

namespace deltix {
namespace dsc {

auto WSCToDSCCodes = [](ulong errcode, int def) {
  switch (errcode) {
    case ERROR_ACCESS_DENIED:               return DSC_ERROR_ACCESS_DENIED;
    case ERROR_DATABASE_DOES_NOT_EXIST:     return DSC_ERROR_DB_NOT_EXIST;
    case ERROR_CIRCULAR_DEPENDENCY:         return DSC_ERROR_CIRCULAR_DEPENDENCY;
    case ERROR_DUPLICATE_SERVICE_NAME:      return DSC_ERROR_DUPLICATE_SERVICE_NAME;
    case ERROR_INVALID_HANDLE:              return DSC_ERROR_INVALID_HANDLE;
    case ERROR_INVALID_NAME:                return DSC_ERROR_INVALID_NAME;
    case ERROR_INVALID_PARAMETER:           return DSC_ERROR_INVALID_PARAMETER;
    case ERROR_INVALID_SERVICE_ACCOUNT:     return DSC_ERROR_INVALID_SERVICE_ACCOUNT;
    case ERROR_SERVICE_EXISTS:              return DSC_ERROR_SERVICE_EXISTS;
    case ERROR_SERVICE_MARKED_FOR_DELETE:   return DSC_ERROR_SERVICE_MARKED_FOR_DELETE;
    case ERROR_INSUFFICIENT_BUFFER:         return DSC_ERROR_INSUFFICIENT_BUFFER;
    case ERROR_INVALID_LEVEL:               return DSC_ERROR_INVALID_LEVEL;
    case ERROR_SHUTDOWN_IN_PROGRESS:        return DSC_ERROR_SERVICE_MARKED_FOR_DELETE;
    case ERROR_FILE_NOT_FOUND:              return DSC_ERROR_BINARY_NOT_FOUND;
    case ERROR_PATH_NOT_FOUND:              return DSC_ERROR_PATH_NOT_FOUND;
    case ERROR_SERVICE_ALREADY_RUNNING:     return DSC_ERROR_SERVICE_ALREADY_RUNNING;
    case ERROR_SERVICE_DATABASE_LOCKED:     return DSC_ERROR_SERVICE_DATABASE_LOCKED;
    case ERROR_SERVICE_DEPENDENCY_DELETED:  return DSC_ERROR_SERVICE_DEPENDENCY_DELETED;
    case ERROR_SERVICE_DEPENDENCY_FAIL:     return DSC_ERROR_SERVICE_DEPENDENCY_FAIL;
    case ERROR_SERVICE_DISABLED:            return DSC_ERROR_SERVICE_DISABLED;
    case ERROR_SERVICE_LOGON_FAILED:        return DSC_ERROR_SERVICE_LOGON_FAILED;
    case ERROR_SERVICE_NO_THREAD:           return DSC_ERROR_SERVICE_NO_THREAD;
    case ERROR_SERVICE_REQUEST_TIMEOUT:     return DSC_ERROR_SERVICE_REQUEST_TIMEOUT;
    case ERROR_SERVICE_DOES_NOT_EXIST:      return DSC_ERROR_SERVICE_DOES_NOT_EXIST;
    default: return def;
  }
};

auto DSCToWSCCodes = [](ulong errcode) {
  switch (errcode) {
    case DSC_ERROR_ACCESS_DENIED:               return ERROR_ACCESS_DENIED;
    case DSC_ERROR_DB_NOT_EXIST:                return ERROR_DATABASE_DOES_NOT_EXIST;
    case DSC_ERROR_CIRCULAR_DEPENDENCY:         return ERROR_CIRCULAR_DEPENDENCY;
    case DSC_ERROR_DUPLICATE_SERVICE_NAME:      return ERROR_DUPLICATE_SERVICE_NAME;
    case DSC_ERROR_INVALID_HANDLE:              return ERROR_INVALID_HANDLE;
    case DSC_ERROR_INVALID_NAME:                return ERROR_INVALID_NAME;
    case DSC_ERROR_INVALID_PARAMETER:           return ERROR_INVALID_PARAMETER;
    case DSC_ERROR_INVALID_SERVICE_ACCOUNT:     return ERROR_INVALID_SERVICE_ACCOUNT;
    case DSC_ERROR_SERVICE_EXISTS:              return ERROR_SERVICE_EXISTS;
    case DSC_ERROR_SERVICE_MARKED_FOR_DELETE:   return ERROR_SERVICE_MARKED_FOR_DELETE;
    case DSC_ERROR_INSUFFICIENT_BUFFER:         return ERROR_INSUFFICIENT_BUFFER;
    case DSC_ERROR_INVALID_LEVEL:               return ERROR_INVALID_LEVEL;
    case DSC_ERROR_SHUTDOWN_IN_PROGRESS:        return ERROR_SERVICE_MARKED_FOR_DELETE;
    case DSC_ERROR_BINARY_NOT_FOUND:            return ERROR_FILE_NOT_FOUND;
    case DSC_ERROR_PATH_NOT_FOUND:              return ERROR_PATH_NOT_FOUND;
    case DSC_ERROR_SERVICE_ALREADY_RUNNING:     return ERROR_SERVICE_ALREADY_RUNNING;
    case DSC_ERROR_SERVICE_DATABASE_LOCKED:     return ERROR_SERVICE_DATABASE_LOCKED;
    case DSC_ERROR_SERVICE_DEPENDENCY_DELETED:  return ERROR_SERVICE_DEPENDENCY_DELETED;
    case DSC_ERROR_SERVICE_DEPENDENCY_FAIL:     return ERROR_SERVICE_DEPENDENCY_FAIL;
    case DSC_ERROR_SERVICE_DISABLED:            return ERROR_SERVICE_DISABLED;
    case DSC_ERROR_SERVICE_LOGON_FAILED:        return ERROR_SERVICE_LOGON_FAILED;
    case DSC_ERROR_SERVICE_NO_THREAD:           return ERROR_SERVICE_NO_THREAD;
    case DSC_ERROR_SERVICE_REQUEST_TIMEOUT:     return ERROR_SERVICE_REQUEST_TIMEOUT;
    case DSC_ERROR_SERVICE_DOES_NOT_EXIST:      return ERROR_SERVICE_DOES_NOT_EXIST;
    default: return (long) errcode;
  }
};

String DSMErrorToString(int retcode) 
{
  String ret = _T("Unknown error.");
  switch (retcode) {
    case DSC_ALL_OK:                          ret = _T("No errors."); break;
    case DSC_NULL_HANDLE:                     ret = _T("NULL handle."); break;
    case DSC_SERVICE_ALREADY_STOPPED:         ret = _T("Service already stopped."); break;
    case DSC_UNKNOWN_ERROR:                   ret = _T("Unknown error."); break;
    case DSC_ERROR_SC_MANAGER_INIT:           ret = _T("DB manager initialization error."); break;
    case DSC_ERROR_ACCESS_DENIED:             ret = _T("The handle does not have the access rights."); break;
    case DSC_ERROR_DB_NOT_EXIST:              ret = _T("The specified database does not exist."); break;
    case DSC_ERROR_CLOSE_HANDLE:              ret = _T("Error while closing handle."); break;
    case DSC_ERROR_CREATE_SERVICE:            ret = _T("Error creating service."); break;
    case DSC_ERROR_CIRCULAR_DEPENDENCY:       ret = _T("A circular service dependency was specified."); break;
    case DSC_ERROR_DUPLICATE_SERVICE_NAME:    ret = _T("The display name already exists in the service control manager database either as a service name or as another display name."); break;
    case DSC_ERROR_INVALID_HANDLE:            ret = _T("The handle is invalid."); break;
    case DSC_ERROR_INVALID_NAME:              ret = _T("The specified service name is invalid."); break;
    case DSC_ERROR_INVALID_PARAMETER:         ret = _T("A parameter that was specified is invalid."); break;
    case DSC_ERROR_INVALID_SERVICE_ACCOUNT:   ret = _T("The user account name specified in the lpServiceStartName parameter does not exist."); break;
    case DSC_ERROR_SERVICE_EXISTS:            ret = _T("The specified service already exists in this database."); break;
    case DSC_ERROR_SERVICE_MARKED_FOR_DELETE: ret = _T("The service has been marked for deletion."); break;
    case DSC_ERROR_OPEN_SERVICE:              ret = _T("Error open service."); break;
    case DSC_ERROR_QUERY_STATUS:              ret = _T("Error query service status."); break;
    case DSC_ERROR_INSUFFICIENT_BUFFER:       ret = _T("The buffer is too small for the SERVICE_STATUS_PROCESS structure. Nothing was written to the structure."); break;
    case DSC_ERROR_INVALID_LEVEL:             ret = _T("The InfoLevel parameter contains an unsupported value."); break;
    case DSC_ERROR_SHUTDOWN_IN_PROGRESS:      ret = _T("The system is shutting down; this function cannot be called."); break;
    case DSC_ERROR_SERVICE_IS_RUNNING:        ret = _T("Service is already running."); break;
    case DSC_ERROR_WAITING_TO_STOP_TIMEOUT:   ret = _T("Stop service timeout."); break;
    case DSC_ERROR_START_SERVICE:             ret = _T("Unknown service start error."); break;
    case DSC_ERROR_PATH_NOT_FOUND:            ret = _T("The service binary file could not be found."); break;
    case DSC_ERROR_SERVICE_ALREADY_RUNNING:   ret = _T("An instance of the service is already running."); break;
    case DSC_ERROR_SERVICE_DATABASE_LOCKED:   ret = _T("The database is locked."); break;
    case DSC_ERROR_SERVICE_DEPENDENCY_DELETED:ret = _T("The service depends on a service that does not exist or has been marked for deletion."); break;
    case DSC_ERROR_SERVICE_DEPENDENCY_FAIL:   ret = _T("The service depends on another service that has failed to start."); break;
    case DSC_ERROR_SERVICE_DISABLED:          ret = _T("The service has been disabled."); break;
    case DSC_ERROR_SERVICE_LOGON_FAILED:      ret = _T("The service did not start due to a logon failure. This error occurs if the service is configured to run under an account that does not have the \"Log on as a service\" right."); break;
    case DSC_ERROR_SERVICE_NO_THREAD:         ret = _T("A thread could not be created for the service."); break;
    case DSC_ERROR_SERVICE_REQUEST_TIMEOUT:   ret = _T("The process for the service was started, but it did not call StartServiceCtrlDispatcher, or the thread that called StartServiceCtrlDispatcher may be blocked in a control handler function."); break;
    case DSC_ERROR_SERVICE_NOT_STARTED:       ret = _T("Service not started."); break;
    case DSC_ERROR_WAITING_TO_START_TIMEOUT:  ret = _T("Start service timeout"); break;
    case DSC_ERROR_GET_DEPEND_SERVICES:       ret = _T("Error getting service depends."); break;
    case DSC_ERROR_STOP_SERVICE:              ret = _T("Stop service error"); break;
    case DSC_ERROR_SERVER_NOT_STOPPED:        ret = _T("Service not stopped."); break;
    case DSC_ERROR_DELETE_SERVICE:            ret = _T("Error deleting service"); break;
    case DSC_ERROR_BINARY_NOT_FOUND:          ret = _T("Service binary not found."); break;
    case DSC_ERROR_CHANGE_CONFIGURATION:      ret = _T("Service change configuration error."); break;
    case DSC_ERROR_SET_DESCRIPTION:           ret = _T("Set service description error."); break;
    case DSC_ERROR_SERVICE_DOES_NOT_EXIST:    ret = _T("The specified service does not exist."); break;
    case DSC_ERROR_DEPENDENT_SERVICES_RUNNING:ret = _T("The service cannot be stopped because other running services are dependent on it."); break;
    case DSC_ERROR_INVALID_SERVICE_CONTROL:   ret = _T("The requested control code is not valid, or it is unacceptable to the service."); break;
    case DSC_ERROR_SERVICE_CANNOT_ACCEPT_CTRL:ret = _T("The requested control code cannot be sent to the service because the state of the service is SERVICE_STOPPED, SERVICE_START_PENDING, or SERVICE_STOP_PENDING."); break;
    case DSC_ERROR_SERVICE_NOT_ACTIVE:        ret = _T("The service has not been started."); break;
  }
  Char errBuf[MAX_STR];
  SPrintf(errBuf, "%d", DSCToWSCCodes(retcode));
  String errorCode = errBuf;
  ret = "Code: " + errorCode + ". " + ret;
  return ret;
}

//-------------------------------------------------------------------------------------

int ServiceManager::Init(IN ulong access /* = SC_MANAGER_ALL_ACCESS*/) 
{
  return InitSCManager(scManager_.GetHandle(), EMPTY_STRING, EMPTY_STRING, access);
}

int ServiceManager::CreateService(
  IN  const String &serviceName,
  IN  const String &displayName,
  IN  ulong access,
  IN  ulong serviceType,
  IN  ulong startType,
  IN  ulong errorControl,
  IN  const String &binaryPathName,
  IN  const String &loadOrderGroup,
  OUT ulong *tagId,
  IN  const String &dependencies,
  IN  const String &accountName,
  IN  const String &accountPass)
{
  std::lock_guard<std::mutex> lock(guard_);

  ServiceManagerHandler scService;

  Char *dependBuf = MakeDoubleNullString(dependencies);

  scService.GetHandle() = ::CreateService( 
        scManager_.GetHandle(),                                             // SCM database 
        serviceName.empty() ? NULL : serviceName.c_str(),                   // name of service 
        displayName.empty() ? NULL : displayName.c_str(),                   // service name to display 
        access,                                                             // desired access 
        serviceType,                                                        // service type 
        startType,                                                          // start type 
        errorControl,                                                       // error control type 
        binaryPathName.empty() ? NULL : binaryPathName.c_str(),             // path to service's binary 
        loadOrderGroup.empty() ? NULL : loadOrderGroup.c_str(),             // load ordering group 
        tagId,                                                              // tag identifier 
        dependBuf,                                                          // dependencies 
        accountName.empty() ? NULL : accountName.c_str(),                   // System account 
        accountPass.empty() ? NULL : accountPass.c_str());                  // password 
 
    delete [] dependBuf;
    
    if (NULL == scService.GetHandle()) {
      return WSCToDSCCodes(::GetLastError(), DSC_ERROR_CREATE_SERVICE); 
    }

    return DSC_ALL_OK;
}

int ServiceManager::StartService(IN const String &id, ulong timeout /* = 10000 */) 
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;

  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK)
    return scService, ret;

  //service is allready running
  if (serviceStatus.dwCurrentState != SERVICE_STOPPED)
    return DSC_ERROR_SERVICE_IS_RUNNING;

  if ((ret = WaitServiceStopping(scService.GetHandle(), timeout)) != DSC_ALL_OK)
    return ret;

  if (!::StartService(scService.GetHandle(), 0, NULL))
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_START_SERVICE);

  WaitServiceStarting(scService.GetHandle(), timeout);

  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  if (serviceStatus.dwCurrentState != SERVICE_RUNNING)
    return DSC_ERROR_SERVICE_NOT_STARTED;

  return DSC_ALL_OK;
}

int ServiceManager::StopService(IN const String &id, ulong timeout /* = 10000 */)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;

  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  if (serviceStatus.dwCurrentState == SERVICE_STOPPED)
    return DSC_SERVICE_ALREADY_STOPPED;

  if (serviceStatus.dwCurrentState == SERVICE_STOP_PENDING)
    if (WaitServiceStopping(scService.GetHandle(), timeout) == DSC_ALL_OK)
      return DSC_ALL_OK;

  StopDependServices(scService.GetHandle());

  // Send a stop code. 
  if (!::ControlService(scService.GetHandle(), SERVICE_CONTROL_STOP, (LPSERVICE_STATUS) &serviceStatus))
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_STOP_SERVICE);

  // Wait service to stop
  ret = WaitServiceStopping(scService.GetHandle(), timeout);

  return ret;
}

int ServiceManager::StartServiceNonBlock(IN const String &id) 
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;

  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  //service is allready running
  if (serviceStatus.dwCurrentState != SERVICE_STOPPED && 
      serviceStatus.dwCurrentState != SERVICE_STOP_PENDING)
    return DSC_ERROR_SERVICE_IS_RUNNING;

  if (!::StartService(scService.GetHandle(), 0, NULL))
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_START_SERVICE);

  return DSC_ALL_OK;
}

int ServiceManager::StopServiceNonBlock(IN const String &id)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;

  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  if (serviceStatus.dwCurrentState == SERVICE_STOPPED ||
      serviceStatus.dwCurrentState == SERVICE_STOP_PENDING)
    return DSC_SERVICE_ALREADY_STOPPED;

  StopDependServices(scService.GetHandle());

  // Send a stop code.
  if (!::ControlService(scService.GetHandle(), SERVICE_CONTROL_STOP, (LPSERVICE_STATUS) &serviceStatus)) {
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_STOP_SERVICE);
  }

  return DSC_ALL_OK;
}

int ServiceManager::DeleteService(IN const String &id) 
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  if (!::DeleteService(scService.GetHandle())) 
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_DELETE_SERVICE);

  Sleep(500); //system is not deleted service immediately (I do not know why)

  return DSC_ALL_OK;
}

int ServiceManager::QueryBinaryPath(IN const String &id, OUT String &binaryPath) 
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  LPQUERY_SERVICE_CONFIG serviceConfig = NULL;
  ret = GetServiceConfig(scService.GetHandle(), serviceConfig);
  if (serviceConfig != NULL) {
    binaryPath = serviceConfig->lpBinaryPathName;
    free(serviceConfig);
  }

  return ret;
}

int ServiceManager::QueryStrState(IN const String &id, OUT String &state)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;
  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  Char stateString[MAX_STR];
  SPrintf(stateString, _T("%s"), StateToStr(serviceStatus.dwCurrentState).c_str());
  state = stateString;
 
  return DSC_ALL_OK;
}

int ServiceManager::QueryStrWin32ExitCode(IN const String &id, OUT String &exitCode)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;
  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  Char exitCodeStr[MAX_STR];
  SPrintf(exitCodeStr, _T("%d"), serviceStatus.dwWin32ExitCode);
  exitCode = exitCodeStr;
 
  return DSC_ALL_OK;
}

int ServiceManager::QueryStrServiceExitCode(IN const String &id, OUT String &exitCode)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;
  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  Char exitCodeStr[MAX_STR];
  SPrintf(exitCodeStr, _T("%d"), serviceStatus.dwServiceSpecificExitCode);
  exitCode = exitCodeStr;
 
  return DSC_ALL_OK;
}

int ServiceManager::QueryStrStatus(IN const String &id, OUT String &status)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_STATUS_PROCESS serviceStatus;
  if ((ret = GetServiceStatus(scService.GetHandle(), serviceStatus)) != DSC_ALL_OK) 
    return ret;

  Char statusString[MAX_STR];
  SPrintf(statusString, _T("SERVICE_NAME: %s\r\n"), id.c_str());
  status += statusString;
  SPrintf(statusString, _T("TYPE\t\t\t: %d\t%s\r\n"), serviceStatus.dwServiceType, ServiceTypeToStr(serviceStatus.dwServiceType).c_str());
  status += statusString;
  SPrintf(statusString, _T("STATE\t\t\t: %d\t%s\r\n"), serviceStatus.dwCurrentState, StateToStr(serviceStatus.dwCurrentState).c_str());
  status += statusString;
  SPrintf(statusString, _T("WIN32_EXIT_CODE\t\t: %d\t(0x%x)\r\n"), serviceStatus.dwWin32ExitCode, serviceStatus.dwWin32ExitCode);
  status += statusString;
  SPrintf(statusString, _T("SERVICE_EXIT_CODE\t: %d\t(0x%x)\r\n"), serviceStatus.dwServiceSpecificExitCode, serviceStatus.dwServiceSpecificExitCode);
  status += statusString;
  SPrintf(statusString, _T("CHECKPOINT\t\t: 0x%x\r\n"), serviceStatus.dwCheckPoint);
  status += statusString;
  SPrintf(statusString, _T("WAIT_HINT\t\t: 0x%x\r\n"), serviceStatus.dwWaitHint);
  status += statusString;
 
  return DSC_ALL_OK;
}

int ServiceManager::SetDescription(IN const String &id, IN const String &description) 
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  SERVICE_DESCRIPTION sd;
  sd.lpDescription = const_cast<char *>(description.c_str());
  if (!::ChangeServiceConfig2(scService.GetHandle(), SERVICE_CONFIG_DESCRIPTION, &sd))
    return WSCToDSCCodes(::GetLastError(), DSC_ERROR_CREATE_SERVICE);
    
  return DSC_ALL_OK;
};

int ServiceManager::AddDependency(IN const String &id, IN String &depend) 
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  String depends;
  if ((ret = GetDepends(scService.GetHandle(), depends)) != DSC_ALL_OK)
    return ret;

  std::size_t found = depends.find(depend);
  if (found == std::string::npos) {
    if (!depends.empty()) depends += "/";
    depends += depend;
    ret = ChangeConfig(scService.GetHandle(), SERVICE_NO_CHANGE, SERVICE_NO_CHANGE, SERVICE_NO_CHANGE, "", "", NULL, depends, "", "", "");
  }

  return ret;
}

bool ServiceManager::ServiceExists(IN const String &id)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) {
    return false;
  }

  return true;
}

int ServiceManager::ChangeConfig(
  IN const String &id,
  IN  ulong serviceType,
  IN  ulong startType,
  IN  ulong errorControl,
  IN  const String &binaryPathName,
  IN  const String &loadOrderGroup,
  OUT ulong *tagId,
  IN  const String &dependencies,
  IN  const String &serviceStartName,
  IN  const String &password,
  IN  const String &displayName)
{
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  ret = ChangeConfig(
          scService.GetHandle(),
          serviceType,
          startType,
          errorControl,
          binaryPathName.empty() ? NULL : binaryPathName.c_str(),
          loadOrderGroup.empty() ? NULL : loadOrderGroup.c_str(),
          NULL,
          dependencies,
          serviceStartName.empty() ? NULL : serviceStartName.c_str(),
          password.empty() ? NULL : password.c_str(),
          displayName.empty() ? NULL : displayName.c_str());

  return ret;
}

int ServiceManager::ChangeFailureActions(
  IN const String &id,
  IN SC_ACTION_TYPE type,
  IN int delay,
  IN const String &command)
{  
  std::lock_guard<std::mutex> lock(guard_);

  int ret = DSC_ALL_OK;
  ServiceManagerHandler scService;

  ret = this->OpenService(scService.GetHandle(), id);
  if (ret != DSC_ALL_OK) 
    return ret;

  //change failure action
  SERVICE_FAILURE_ACTIONS sfActions;
  sfActions.dwResetPeriod = INFINITE;
  sfActions.lpRebootMsg = NULL;
  sfActions.lpCommand = const_cast<char *>(command.c_str());

  //actions
  SC_ACTION actions[1];
  actions[0].Delay = delay;
  actions[0].Type = type;
  sfActions.cActions = 1;
  sfActions.lpsaActions = actions;

  ret = ChangeFailureActions(scService.GetHandle(), sfActions);

  return ret;
}

//-------------------------------------------------------------------------------------

int ServiceManager::InitSCManager(IN SC_HANDLE &scManager,
                                   IN const String &host      /* = L"" */, 
                                   IN const String &dbname    /* = L"" */, 
                                   IN ulong access            /* = SC_MANAGER_ALL_ACCESS */) 
{
  CloseServiceHandle(scManager);

  scManager = ::OpenSCManager( 
        host.empty()    ? NULL : host.c_str(),      // computer
        dbname.empty()  ? NULL : dbname.c_str(),    // database 
        access);                                    // access rights 

  if (NULL == scManager) {
    return WSCToDSCCodes(::GetLastError(), DSC_ERROR_SC_MANAGER_INIT);
  }

  return DSC_ALL_OK;
}

int ServiceManager::OpenService(OUT SC_HANDLE &scService,
                                 IN  const String &serviceName,
                                 IN  ulong access /* = SC_MANAGER_ALL_ACCESS */)
{
  scService = ::OpenService(scManager_.GetHandle(), serviceName.c_str(), access);

  if (NULL == scService) {
    return WSCToDSCCodes(::GetLastError(), DSC_ERROR_OPEN_SERVICE);
  }

  return DSC_ALL_OK;
}

int ServiceManager::CloseServiceHandle(IN SC_HANDLE &handle) 
{
  if (handle != NULL) {
    bool ret = ::CloseServiceHandle(handle);
    handle = NULL;
    return ret ? DSC_ALL_OK : DSC_ERROR_CLOSE_HANDLE;
  }

  return DSC_NULL_HANDLE;
}

int ServiceManager::ChangeConfig(
  IN SC_HANDLE &scService,
  IN  ulong serviceType,
  IN  ulong startType,
  IN  ulong errorControl,
  IN  const String &binaryPathName,
  IN  const String &loadOrderGroup,
  OUT ulong *tagId,
  IN  const String &dependencies,
  IN  const String &serviceStartName,
  IN  const String &password,
  IN  const String &displayName)
{
  int ret = DSC_ALL_OK;

  Char *dependBuf = MakeDoubleNullString(dependencies);

  if (!::ChangeServiceConfig(
          scService,
          serviceType,
          startType,
          errorControl,
          binaryPathName.empty() ? NULL : binaryPathName.c_str(),
          loadOrderGroup.empty() ? NULL : loadOrderGroup.c_str(),
          NULL,
          dependBuf,
          serviceStartName.empty() ? NULL : serviceStartName.c_str(),
          password.empty() ? NULL : password.c_str(),
          displayName.empty() ? NULL : displayName.c_str()))
  {
    delete [] dependBuf;
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_CHANGE_CONFIGURATION);
  }

  delete [] dependBuf;

  return ret;
}

int ServiceManager::ChangeFailureActions(
  IN SC_HANDLE &scService,
  IN SERVICE_FAILURE_ACTIONS sfActions)
{
  if (!::ChangeServiceConfig2(
        scService,                        // handle to service
        SERVICE_CONFIG_FAILURE_ACTIONS,   // change: description
        &sfActions))                      // new description
      return DSC_ERROR_CHANGE_CONFIGURATION;
    else 
      return DSC_ALL_OK;
}

int ServiceManager::GetServiceStatus(IN SC_HANDLE &scService, OUT SERVICE_STATUS_PROCESS &serviceStatus)
{
  ulong size;

  if (!::QueryServiceStatusEx(scService, SC_STATUS_PROCESS_INFO, 
    (LPBYTE) &serviceStatus, sizeof(SERVICE_STATUS_PROCESS), &size)) {
    return WSCToDSCCodes(GetLastError(), DSC_ERROR_QUERY_STATUS);
  }

  return DSC_ALL_OK;
}

int ServiceManager::GetServiceConfig(IN SC_HANDLE &scService, OUT LPQUERY_SERVICE_CONFIG &serviceConfig)
{
  ulong cbBufSize, dwBytesNeeded, dwError;

  //get structure size
  if (!QueryServiceConfig(scService, NULL, 0, &dwBytesNeeded)) {
    dwError = GetLastError();
    if (ERROR_INSUFFICIENT_BUFFER == dwError) {
      cbBufSize = dwBytesNeeded;
      serviceConfig = (LPQUERY_SERVICE_CONFIG) malloc(cbBufSize);
      if (!QueryServiceConfig(scService, serviceConfig, cbBufSize, &dwBytesNeeded)) {
        return WSCToDSCCodes(GetLastError(), DSC_ERROR_GET_QUERY_CONFIG);
      }
    } else {
      return WSCToDSCCodes(GetLastError(), DSC_ERROR_GET_QUERY_CONFIG);
    }
  }
  
  return DSC_ALL_OK;
}

int ServiceManager::WaitServiceStopping(IN SC_HANDLE &scService, IN ulong timeout, IN ulong minWait /* = 100 */, IN ulong maxWait /* = 10000 */) 
{
  int ret = DSC_ALL_OK;
  SERVICE_STATUS_PROCESS serviceStatus;

  // Check the status until the service is no longer stop pending. 
  if ((ret = GetServiceStatus(scService, serviceStatus)) != DSC_ALL_OK) 
    return ret; 

  ulong startTickCount = GetTickCount();
  
  //wait service to stop before attempting it to start
  while (serviceStatus.dwCurrentState != SERVICE_STOPPED) {
    // Do not wait longer than the wait hint. A good interval is 
    // one-tenth of the wait hint but not less than min millis  
    // and not more than max millis. 
    ulong waitTime = serviceStatus.dwWaitHint / 10;
    if (waitTime < minWait) waitTime = minWait;
    else if (waitTime > maxWait) waitTime = maxWait;
    Sleep(waitTime);

    // Check the status until the service is no longer stop pending. 
    if ((ret = GetServiceStatus(scService, serviceStatus)) != DSC_ALL_OK) 
      return ret; 

    if (serviceStatus.dwCurrentState == SERVICE_STOPPED) 
      return ret;

    if (GetTickCount() - startTickCount > timeout)
      return DSC_ERROR_WAITING_TO_STOP_TIMEOUT;
  }

  return ret;
}

int ServiceManager::WaitServiceStarting(IN SC_HANDLE &scService, IN ulong timeout, IN ulong minWait /* = 100 */, IN ulong maxWait /* = 10000 */)
{
  int ret = DSC_ALL_OK;
  SERVICE_STATUS_PROCESS serviceStatus;

  // Check the status until the service is no longer stop pending. 
  if ((ret = GetServiceStatus(scService, serviceStatus)) != DSC_ALL_OK) 
    return ret; 

  ulong oldCheckPoint = serviceStatus.dwCheckPoint;
  ulong startTickCount = GetTickCount();
  
  while (serviceStatus.dwCurrentState == SERVICE_START_PENDING) 
  { 
    // Do not wait longer than the wait hint. A good interval is 
    // one-tenth of the wait hint but not less than 1 second  
    // and not more than 10 seconds. 
    ulong waitTime = serviceStatus.dwWaitHint / 10;
    if (waitTime < minWait) waitTime = minWait;
    else if (waitTime > maxWait) waitTime = maxWait;
    Sleep(waitTime);

    // Check the status again. 
    if ((ret = GetServiceStatus(scService, serviceStatus)) != DSC_ALL_OK) 
      return ret; 
 
    if (serviceStatus.dwCheckPoint > oldCheckPoint) {
      // Continue to wait and check.
      startTickCount = GetTickCount();
      oldCheckPoint = serviceStatus.dwCheckPoint;
    } else {
      if (GetTickCount() - startTickCount > timeout)
          // No progress made within the wait hint.
          break;
    }
  } 

  return ret;
}

int ServiceManager::StopDependServices(IN SC_HANDLE &scService)
{
  int ret = DSC_ALL_OK;

  ulong bytesNeeded;
  ulong count;
  LPENUM_SERVICE_STATUS dependencies = NULL;
  ENUM_SERVICE_STATUS dependServiceStatus;
  SERVICE_STATUS_PROCESS serviceStatus;

  // Pass a zero-length buffer to get the required buffer size.
  if (::EnumDependentServices(scService, SERVICE_ACTIVE, dependencies, 0, &bytesNeeded, &count)) {
    // If the Enum call succeeds, then there are no dependent
    // services, so do nothing.
    return DSC_ALL_OK;
  } else {
    if (GetLastError() != ERROR_MORE_DATA)
      return WSCToDSCCodes(GetLastError()); // Unexpected error

    // Allocate a buffer for the dependencies.
    std::string strBuf;
    strBuf.resize(bytesNeeded, '\0');
    dependencies = (LPENUM_SERVICE_STATUS) strBuf.c_str();  

    // Enumerate the dependencies.
    if (!::EnumDependentServices(scService, SERVICE_ACTIVE, dependencies, bytesNeeded, &bytesNeeded, &count))
      return DSC_ERROR_GET_DEPEND_SERVICES;

    for (int i = 0; i < count; i++) {
      dependServiceStatus = *(dependencies + i);
      // Open the service.
      SC_HANDLE scDepend;
      ret = OpenService(scDepend, dependServiceStatus.lpServiceName, SERVICE_STOP | SERVICE_QUERY_STATUS );
      if (ret != DSC_ALL_OK) {
        CloseServiceHandle(scDepend);
        continue;
      }
      
      // Send a stop code.
      if (!::ControlService(scDepend, SERVICE_CONTROL_STOP, (LPSERVICE_STATUS) &serviceStatus)) {
        CloseServiceHandle(scDepend);
        continue;
      }

      // Wait service to stop
      WaitServiceStopping(scDepend, 10000);

      // Close service handle
      CloseServiceHandle(scDepend);
    } 
  }

  return DSC_ALL_OK;
}

int ServiceManager::GetDepends(IN SC_HANDLE &scService, OUT String &depends)
{
  int ret = DSC_ALL_OK;
  depends = "";

  std::string strBuf;
  LPQUERY_SERVICE_CONFIG serviceConfig = NULL;
  ulong bytesNeeded, bufSize, error;

  if (!::QueryServiceConfig(scService, NULL, 0, &bytesNeeded)) {
    if (ERROR_INSUFFICIENT_BUFFER != ::GetLastError()) 
      return WSCToDSCCodes(::GetLastError(), DSC_ERROR_GET_QUERY_CONFIG); 

    bufSize = bytesNeeded;
    strBuf.resize(bufSize, '\0');
    serviceConfig = (LPQUERY_SERVICE_CONFIG) strBuf.c_str();
  }

  if (!::QueryServiceConfig(scService, serviceConfig, bufSize, &bytesNeeded))
    return WSCToDSCCodes(::GetLastError(), DSC_ERROR_GET_QUERY_CONFIG); 

  depends = FromDoubleNullString(serviceConfig->lpDependencies);

  return DSC_ALL_OK;
}


String ServiceManager::FromDoubleNullString(IN Char *dnstr)
{
  String substr;
  String outstr;
  Char *ptr = dnstr;

  while (*ptr != NULL) {
    substr = ptr;
    ptr += substr.length() * sizeof(Char) + 1 * sizeof(Char);
    outstr += substr;
    if (*ptr != NULL)
      outstr += "/";
  }

  return outstr;
}

Char * ServiceManager::MakeDoubleNullString(IN const String &depends)
{  
  std::vector<String> ardepends;
  String tmp(depends);

  int bufferSize = 0;
  while (!tmp.empty()) {
    size_t pos = tmp.find(L'/');
    if (pos == String::npos) {
      //last depend
      ardepends.push_back(tmp);
      bufferSize += tmp.length() + 1;
      tmp = EMPTY_STRING;
    } else {
      //found new depend
      String depend = tmp.substr(0, pos);
      ardepends.push_back(depend);
      bufferSize += depend.length() + 1;
      tmp = tmp.substr(pos + 1, tmp.length() - (pos + 1));
    }
  }
  bufferSize += 1;
  
  Char *buffer = new Char[bufferSize];
  memset(buffer, 0, bufferSize * sizeof(Char));

  int pos = 0;
  for (int i = 0; i < ardepends.size(); ++i) {
    memcpy((buffer + pos), ardepends[i].c_str(), ardepends[i].length()*sizeof(Char));
    pos += ardepends[i].length() + 1 /*null term*/;
  }
  
  return buffer;
}

int ServiceManager::WSCToDSCCodes(IN ulong errcode, IN int def /* = DSC_UNKNOWN_ERROR */) 
{
  int retcode = def;

  switch (errcode) {
    case ERROR_ACCESS_DENIED:               retcode = DSC_ERROR_ACCESS_DENIED;              break;
    case ERROR_DATABASE_DOES_NOT_EXIST:     retcode = DSC_ERROR_DB_NOT_EXIST;               break;
    case ERROR_CIRCULAR_DEPENDENCY:         retcode = DSC_ERROR_CIRCULAR_DEPENDENCY;        break;
    case ERROR_DUPLICATE_SERVICE_NAME:      retcode = DSC_ERROR_DUPLICATE_SERVICE_NAME;     break;
    case ERROR_INVALID_HANDLE:              retcode = DSC_ERROR_INVALID_HANDLE;             break;
    case ERROR_INVALID_NAME:                retcode = DSC_ERROR_INVALID_NAME;               break;
    case ERROR_INVALID_PARAMETER:           retcode = DSC_ERROR_INVALID_PARAMETER;          break;
    case ERROR_INVALID_SERVICE_ACCOUNT:     retcode = DSC_ERROR_INVALID_SERVICE_ACCOUNT;    break;
    case ERROR_SERVICE_EXISTS:              retcode = DSC_ERROR_SERVICE_EXISTS;             break;
    case ERROR_SERVICE_MARKED_FOR_DELETE:   retcode = DSC_ERROR_SERVICE_MARKED_FOR_DELETE;  break;
    case ERROR_INSUFFICIENT_BUFFER:         retcode = DSC_ERROR_INSUFFICIENT_BUFFER;        break;
    case ERROR_INVALID_LEVEL:               retcode = DSC_ERROR_INVALID_LEVEL;              break;
    case ERROR_SHUTDOWN_IN_PROGRESS:        retcode = DSC_ERROR_SERVICE_MARKED_FOR_DELETE;  break;
    case ERROR_FILE_NOT_FOUND:              retcode = DSC_ERROR_BINARY_NOT_FOUND;           break;
    case ERROR_PATH_NOT_FOUND:              retcode = DSC_ERROR_PATH_NOT_FOUND;             break;
    case ERROR_SERVICE_ALREADY_RUNNING:     retcode = DSC_ERROR_SERVICE_ALREADY_RUNNING;    break;
    case ERROR_SERVICE_DATABASE_LOCKED:     retcode = DSC_ERROR_SERVICE_DATABASE_LOCKED;    break;
    case ERROR_SERVICE_DEPENDENCY_DELETED:  retcode = DSC_ERROR_SERVICE_DEPENDENCY_DELETED; break;
    case ERROR_SERVICE_DEPENDENCY_FAIL:     retcode = DSC_ERROR_SERVICE_DEPENDENCY_FAIL;    break;
    case ERROR_SERVICE_DISABLED:            retcode = DSC_ERROR_SERVICE_DISABLED;           break;
    case ERROR_SERVICE_LOGON_FAILED:        retcode = DSC_ERROR_SERVICE_LOGON_FAILED;       break;
    case ERROR_SERVICE_NO_THREAD:           retcode = DSC_ERROR_SERVICE_NO_THREAD;          break;
    case ERROR_SERVICE_REQUEST_TIMEOUT:     retcode = DSC_ERROR_SERVICE_REQUEST_TIMEOUT;    break;
    case ERROR_SERVICE_DOES_NOT_EXIST:      retcode = DSC_ERROR_SERVICE_DOES_NOT_EXIST;     break;
    case ERROR_DEPENDENT_SERVICES_RUNNING:  retcode = DSC_ERROR_DEPENDENT_SERVICES_RUNNING; break;
    case ERROR_INVALID_SERVICE_CONTROL:     retcode = DSC_ERROR_INVALID_SERVICE_CONTROL;    break;
    case ERROR_SERVICE_CANNOT_ACCEPT_CTRL:  retcode = DSC_ERROR_SERVICE_CANNOT_ACCEPT_CTRL; break;
    case ERROR_SERVICE_NOT_ACTIVE:          retcode = DSC_ERROR_SERVICE_NOT_ACTIVE;         break;
  }

  return retcode;
}

String ServiceManager::ServiceTypeToStr(IN ulong type)
{
  String retType = _T("UNKNOWN");

  switch (type) {
  case SERVICE_FILE_SYSTEM_DRIVER:        retType = _T("SERVICE_FILE_SYSTEM_DRIVER");          break;
  case SERVICE_KERNEL_DRIVER:             retType = _T("SERVICE_KERNEL_DRIVER");               break;
  case SERVICE_WIN32_OWN_PROCESS:         retType = _T("SERVICE_WIN32_OWN_PROCESS");           break;
  case SERVICE_WIN32_SHARE_PROCESS:       retType = _T("SERVICE_WIN32_SHARE_PROCESS");         break;
  }

  return retType;
}

String ServiceManager::StateToStr(IN ulong status) 
{
  String retStatus = _T("UNKNOWN");

  switch (status) {
  case SERVICE_CONTINUE_PENDING:          retStatus = _T("CONTINUE_PENDING");                break;
  case SERVICE_PAUSE_PENDING:             retStatus = _T("PAUSE_PENDING");                   break;
  case SERVICE_PAUSED:                    retStatus = _T("PAUSED");                          break;
  case SERVICE_RUNNING:                   retStatus = _T("RUNNING");                         break;
  case SERVICE_START_PENDING:             retStatus = _T("START_PENDING");                   break;
  case SERVICE_STOP_PENDING:              retStatus = _T("STOP_PENDING");                    break;
  case SERVICE_STOPPED:                   retStatus = _T("STOPPED");                         break;
  }

  return retStatus;
}

//-------------------------------------------------------------------------------------

} //nm dsc
} //nm deltix