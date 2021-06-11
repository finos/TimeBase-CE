#include "DSCCommandArgs.h"

namespace deltix {
namespace dsc {

Char *_commands_[][2] = {
  { DSC_COMMAND_CREATE, _T("Create new service. Use <options> to set configuration.") },
  { DSC_COMMAND_DELETE,   _T("Delete existing service.") },
  { DSC_COMMAND_START,  _T("Start existing service.") },
  { DSC_COMMAND_STOP,   _T("Stop running service.") },
  { DSC_COMMAND_QUERY,   _T("Query service information.") },
  { DSC_COMMAND_CONFIG,   _T("Change service configuration. Use <options>.") },
  { NULL, NULL }
};

Char *_options_[][2] = {
  { DSC_TYPE_ARG,               _T("\t{own|share|kernel|filesys|rec|adapt|interact type= {own|share}} : Specifies the service type.") },
  { DSC_START_ARG,              _T("\t{boot|system|auto|demand|disabled} : Specifies the start type for the service.") },
  { DSC_ERROR_ARG,              _T("\t{normal|severe|critical|ignore} : Specifies the severity of the error if the service fails to start during boot.") },
  { DSC_BINPATH_ARG,            _T("\tBinaryPathName : Specifies a path to the service binary file.") },
  { DSC_GROUP_ARG,              _T("\tLoadOrderGroup : Specifies the name of the group of which this service is a member. The list of groups is stored in the registry in the HKLM\System\CurrentControlSet\Control\ServiceGroupOrder subkey. The default is null.") },
  { DSC_TAG_ARG,                _T("\t\t{yes|no} : Specifies whether or not to obtain a TagID from the CreateService call. Tags are only used for boot-start and system-start drivers.") },
  { DSC_DEPEND_ARG,             _T("\tdependencies : Specifies the names of services or groups which must start before this service. The names are separated by forward slashes (/).") },
  { DSC_OBJ_ARG,                _T("\t\t{AccountName|ObjectName} : Specifies a name of an account in which a service will run, or specifies a name of the Windows driver object in which the driver will run. The default is LocalSystem.") },
  { DSC_DISPNAME_ARG,           _T("\tDisplayName : Specifies a friendly, meaningful name that can be used in user-interface programs to identify the service to users. For example, the subkey name of one service is wuauserv, which is not be helpful to the user, and the display name is Automatic Updates.") },
  { DSC_PASSWORD_ARG,           _T("\tPassword : Specifies a password. This is required if an account other than the LocalSystem account is used.") },
  { NULL, NULL }
};

int DSCCommandArgs::Init(int argc, Char *argv[])
{
  if (argc < 3) {
    WriteCommandInfo();
    return -1;
  }

  auto tolow = [](String str) {
    for (int i = 0; i < str.length(); ++i) 
      str[i] = TOLOWER(str[i]);
    return str;
  };

  int curArgNum = 1;
  String curArg = argv[1];
  if (curArg.find(_T("\\\\")) == 0) {
    //set server name
    args_[DSC_SERVER] = argv[1];
    args_[DSC_COMMAND] = tolow(argv[2]);
    if (argc < 3) {
      WriteCommandInfo();
      return -1;
    }
    args_[DSC_SERVICE_NAME] = argv[3];
    curArgNum = 4;
  } else {
    //set command
    args_[DSC_COMMAND] = tolow(argv[1]);
    args_[DSC_SERVICE_NAME] = argv[2];
    curArgNum = 3;
  }

  //fill other arguments
  //  argname= argval
  for (; curArgNum < argc - 1; curArgNum += 2) {
    String argName = tolow(argv[curArgNum]);
    args_[argName] = argv[curArgNum + 1];
  }

  return Size();
}

int DSCCommandArgs::Size() const
{
  return args_.size();
}

String DSCCommandArgs::GetValue(const String &name) const
{
  auto arg = args_.find(name);
  if (arg != args_.end())
    return arg->second;

  return NULL_STRING;
}

bool DSCCommandArgs::IsExists(const String &name) const
{
  auto arg = args_.find(name);
  if (arg != args_.end())
    return true;

  return false;
}

void DSCCommandArgs::WriteCommandInfo() const
{
  COUT << DSC_USAGE_MSG << std::endl;

  COUT << std::endl << _T("Commands:") << std::endl;
  int i = 0; 
  while (_commands_[i][0] != NULL) {
    COUT << "\t" << _commands_[i][0] << " - " << _commands_[i][1] << std::endl;
    ++i;
  }

  COUT << std::endl << _T("<ptions>:") << std::endl << std::endl;
  i = 0;
  while (_options_[i][0] != NULL) {
    COUT << "\t<" << _options_[i][0] << "> " << _options_[i][1] << std::endl << std::endl;
    ++i;
  }
}

} //nm dsc
} //nm deltix