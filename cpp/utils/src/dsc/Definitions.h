#ifndef _DEFINITIONS_H_
#define _DEFINITIONS_H_

#include <stdint.h>

#include <iostream>
#include <string>
#include <vector>

namespace deltix {

#ifdef _UNICODE
  #define Main                     wmain
  #define IToA                     _itow
  #define SPrintf                  swprintf
  #define TOLOWER                  towlower
  #define COUT                     std::wcout

  #define _T(s)                    L##s

  typedef std::wstring             String;
  typedef wchar_t                  Char;
#else
  #define Main                     main
  #define IToA                     itoa
  #define SPrintf                  sprintf
  #define TOLOWER                  tolower
  #define COUT                     std::cout

  #define _T(s)                    s

  typedef std::string              String;
  typedef char                     Char;
#endif

#define MAX_STR                  2048

#define EMPTY_STRING             _T("")
#define NULL_STRING              _T("\0")
#define PATH_DELIM               _T("/")
#define CR                       _T("\r")
#define LF                       _T("\n")
#define TB                       _T("\t")

#define IN
#define OUT
#define IN_OUT

typedef unsigned long            ulong;

} //nm deltix

#endif //_DEFINITIONS_H_