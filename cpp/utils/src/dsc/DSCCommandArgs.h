#ifndef _DSC_COMMAND_ARGS_H_
#define _DSC_COMMAND_ARGS_H_

#include <string>
#include <map>

#include "Definitions.h"

namespace deltix {
namespace dsc {

//arg names
#define DSC_SERVER              _T("server")
#define DSC_COMMAND             _T("command")
#define DSC_SERVICE_NAME        _T("service_name")

#define DSC_COMMAND_CREATE      _T("create")
#define DSC_COMMAND_START       _T("start")
#define DSC_COMMAND_STOP        _T("stop")
#define DSC_COMMAND_DELETE      _T("delete")
#define DSC_COMMAND_QUERY       _T("query")
#define DSC_COMMAND_CONFIG      _T("config")

#define DSC_TYPE_ARG            _T("type=")
#define DSC_TYPE_VAL_OWN        _T("own")
#define DSC_TYPE_VAL_SHARE      _T("share")
#define DSC_TYPE_VAL_KERNEL     _T("kernel")
#define DSC_TYPE_VAL_FILESYS    _T("filesys")
#define DSC_TYPE_VAL_REC        _T("rec")
#define DSC_TYPE_VAL_ADAPT      _T("adapt")
#define DSC_TYPE_VAL_INTERACT   _T("interact")

#define DSC_START_ARG           _T("start=")
#define DSC_START_VAL_BOOT      _T("boot")
#define DSC_START_VAL_SYSTEM    _T("system")
#define DSC_START_VAL_AUTO      _T("auto")
#define DSC_START_VAL_DEMAND    _T("demand")
#define DSC_START_VAL_DISABLED  _T("disabled")

#define DSC_ERROR_ARG           _T("error=")
#define DSC_ERROR_VAL_NORMAL    _T("normal")
#define DSC_ERROR_VAL_SEVERE    _T("severe")
#define DSC_ERROR_VAL_CRITICAL  _T("cretical")
#define DSC_ERROR_VAL_IGNORE    _T("ignore")

#define DSC_BINPATH_ARG         _T("binpath=")
#define DSC_GROUP_ARG           _T("group=")
#define DSC_TAG_ARG             _T("tag=")
#define DSC_DEPEND_ARG          _T("depend=")
#define DSC_OBJ_ARG             _T("obj=")
#define DSC_OBJ_VAL_ACCNAME     _T("accountname")
#define DSC_OBJ_VAL_OBJNAME     _T("objectname")
#define DSC_DISPNAME_ARG        _T("displayname=")
#define DSC_PASSWORD_ARG        _T("password=")

#define DSC_YES_VAL             _T("yes")
#define DSC_YES_NO              _T("no")

#define DSC_USAGE_MSG           _T("USAGE:\r\n\tdsc <\\\\server> command service_name <options>")

typedef std::map<String, String> ArgsMap;

class DSCCommandArgs {
public:
  DSCCommandArgs() { };

  int Init(int argc, Char *argv[]);
  String GetValue(const String &name) const;
  int Size() const;
  bool IsExists(const String &name) const;

  void WriteCommandInfo() const;

private:
  ArgsMap args_;
};

} //nm dsc
} //nm deltix

#endif //_DSC_COMMAND_ARGS_H_