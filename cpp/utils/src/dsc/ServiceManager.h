#ifndef _SERVICE_CONTROL_H_
#define _SERVICE_CONTROL_H_

#include <Windows.h>

#include <mutex>

#include "Definitions.h"

namespace deltix {
namespace dsc {

#define DSC_ALL_OK                             0 
#define DSC_NULL_HANDLE                        1
#define DSC_SERVICE_ALREADY_STOPPED            2

#define DSC_UNKNOWN_ERROR                     -1
#define DSC_ERROR_SC_MANAGER_INIT             -2
#define DSC_ERROR_ACCESS_DENIED               -3
#define DSC_ERROR_DB_NOT_EXIST                -4
#define DSC_ERROR_CLOSE_HANDLE                -5

#define DSC_ERROR_CREATE_SERVICE              -6
#define DSC_ERROR_CIRCULAR_DEPENDENCY         -7
#define DSC_ERROR_DUPLICATE_SERVICE_NAME      -8
#define DSC_ERROR_INVALID_HANDLE              -9
#define DSC_ERROR_INVALID_NAME                -10
#define DSC_ERROR_INVALID_PARAMETER           -11
#define DSC_ERROR_INVALID_SERVICE_ACCOUNT     -12
#define DSC_ERROR_SERVICE_EXISTS              -13
#define DSC_ERROR_SERVICE_MARKED_FOR_DELETE   -14

#define DSC_ERROR_OPEN_SERVICE                -15
#define DSC_ERROR_QUERY_STATUS                -16
#define DSC_ERROR_INSUFFICIENT_BUFFER         -17
#define DSC_ERROR_INVALID_LEVEL               -18
#define DSC_ERROR_SHUTDOWN_IN_PROGRESS        -19

#define DSC_ERROR_SERVICE_IS_RUNNING          -20
#define DSC_ERROR_WAITING_TO_STOP_TIMEOUT     -21
#define DSC_ERROR_START_SERVICE               -22
#define DSC_ERROR_PATH_NOT_FOUND              -23
#define DSC_ERROR_SERVICE_ALREADY_RUNNING     -24
#define DSC_ERROR_SERVICE_DATABASE_LOCKED     -25
#define DSC_ERROR_SERVICE_DEPENDENCY_DELETED  -26
#define DSC_ERROR_SERVICE_DEPENDENCY_FAIL     -27
#define DSC_ERROR_SERVICE_DISABLED            -28
#define DSC_ERROR_SERVICE_LOGON_FAILED        -29
#define DSC_ERROR_SERVICE_NO_THREAD           -30
#define DSC_ERROR_SERVICE_REQUEST_TIMEOUT     -31
#define DSC_ERROR_SERVICE_NOT_STARTED         -32
#define DSC_ERROR_WAITING_TO_START_TIMEOUT    -33
#define DSC_ERROR_GET_DEPEND_SERVICES         -34
#define DSC_ERROR_STOP_SERVICE                -35
#define DSC_ERROR_SERVER_NOT_STOPPED          -36
#define DSC_ERROR_DELETE_SERVICE              -37
#define DSC_ERROR_BINARY_NOT_FOUND            -38

#define DSC_ERROR_CHANGE_CONFIGURATION        -39
#define DSC_ERROR_SET_DESCRIPTION             -40
#define DSC_ERROR_GET_QUERY_CONFIG            -41
#define DSC_ERROR_SERVICE_DOES_NOT_EXIST      -42

#define DSC_ERROR_DEPENDENT_SERVICES_RUNNING  -43
#define DSC_ERROR_INVALID_SERVICE_CONTROL     -44
#define DSC_ERROR_SERVICE_CANNOT_ACCEPT_CTRL  -45
#define DSC_ERROR_SERVICE_NOT_ACTIVE          -46

class ServiceManagerHandler {
private:
  SC_HANDLE handle_;

public:
  ServiceManagerHandler() {
    handle_ = NULL;
  }

  ~ServiceManagerHandler() {
    if (handle_ != NULL) {
      ::CloseServiceHandle(handle_);
      handle_ = NULL;
    }
  }

  SC_HANDLE & GetHandle() { return handle_; }
};

class ServiceManager {
private:
  ServiceManagerHandler   scManager_;
  std::mutex              guard_;

  //non copyable
  ServiceManager(const ServiceManager &) { };
  ServiceManager & operator=(const ServiceManager &) { };

public:
  ServiceManager() { };

  //Init service control manager
  int Init(IN ulong access = SC_MANAGER_ALL_ACCESS);

  //Create new service
  int CreateService(
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
    IN  const String &accountPass
  );

  //Start opened service
  int StartService(IN const String &id, ulong timeout = 10000);

  //Stop running service
  int StopService(IN const String &id, ulong timeout = 10000);

  //Start opened service without delay
  int StartServiceNonBlock(IN const String &id);

  //Stop running service without delay
  int StopServiceNonBlock(IN const String &id);

  //Delete Existing service
  int DeleteService(IN const String &id);

  //Returns image path (or binary path, or executable of service)
  int QueryBinaryPath(IN const String &id, OUT String &binaryPath);

  //Returns service state string
  int QueryStrState(IN const String &id, OUT String &state);

  //Returns service status string
  int QueryStrStatus(IN const String &id, OUT String &status);

  //Returns win32 exit code string
  int QueryStrWin32ExitCode(IN const String &id, OUT String &exitCode);

  //Returns service exit code string
  int QueryStrServiceExitCode(IN const String &id, OUT String &exitCode);

  //set description of service
  int SetDescription(IN const String &id, IN const String &description);

  //returns service dependencies
  int AddDependency(IN const String &id, IN String &depend);

  //return true if service exists
  bool ServiceExists(IN const String &id);

  int ChangeConfig(
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
    IN  const String &displayName
  );

  int ChangeFailureActions(
    IN const String &id,
    IN SC_ACTION_TYPE type,
    IN int delay,
    IN const String &command
  );

private:
  //Service control utils:
  //  Inits service control manager
  int InitSCManager(OUT SC_HANDLE &scManager,
                    IN  const String &host      = EMPTY_STRING, 
                    IN  const String &dbname    = EMPTY_STRING, 
                    IN  ulong access            = SC_MANAGER_ALL_ACCESS);

  //  Opens service
  int OpenService(OUT SC_HANDLE &scService,
                   IN  const String &serviceName,
                   IN  ulong access = SC_MANAGER_ALL_ACCESS);

  //  Close handle
  int CloseServiceHandle(IN SC_HANDLE &handle);

  int ChangeConfig(
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
    IN  const String &displayName
  );

  int ChangeFailureActions(
    IN SC_HANDLE &scService,
    IN SERVICE_FAILURE_ACTIONS sfActions
  );

  //  Returns structure with service status
  int GetServiceStatus(IN SC_HANDLE &scService, OUT SERVICE_STATUS_PROCESS &serviceStatus);

  //  Returns structure with service config
  //    !!!must free LPQUERY_SERVICE_CONFIG &serviceConfig after using!!!
  int GetServiceConfig(IN SC_HANDLE &scService, OUT LPQUERY_SERVICE_CONFIG &serviceConfig);

  //  Wait pending service
  int WaitServiceStopping(IN SC_HANDLE &scService, IN ulong timeout, IN ulong minWait = 100, IN ulong maxWait = 10000);
  int WaitServiceStarting(IN SC_HANDLE &scService, IN ulong timeout, IN ulong minWait = 100, IN ulong maxWait = 10000);

  //  Stop all depends services
  int StopDependServices(IN SC_HANDLE &scService);

  int GetDepends(IN SC_HANDLE &scService, OUT String &depends);

  //  Convert double null terminated string to string (separated by / for dependencies)
  String FromDoubleNullString(IN Char *dnstr);

  //Other utils:
  //  Makes double null string from vector of strings (for some parameters)
  //  returned string must be cleaned after usage
  Char * MakeDoubleNullString(IN const String &arstr);

  //  Translates WinAPI Service Control error codes to Deltix Service Control error codes
  int WSCToDSCCodes(IN ulong errcode, IN int default1 = DSC_UNKNOWN_ERROR);

  //  Returns string value of status code
  String StateToStr(IN ulong status);
  String ServiceTypeToStr(IN ulong type);
};

//return text of errorcode
String DSMErrorToString(int retcode);

} //nm dsc
} //nm deltix

#endif //_DELTIX_SERVICE_CONTROL_H_