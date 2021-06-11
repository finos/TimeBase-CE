#include <windows.h>
#include <jni.h>
#include <string>
#include <fstream>
#include <sstream>
#include <iostream>
#include <string.h>
#include <ctime>

using namespace std;

static const char *			JVM_KEY = "jvm=";
static const char *			CLASS_KEY = "class=";
static const char *			CP_KEY = "cp=";
static const char *			LIB_KEY = "lib=";

typedef jint (JNICALL *t_CreateJavaVM) (JavaVM **pvm, void **penv, void *args);

static string			mJVMDLLPath;
static string			mClassName;
static ostringstream	mClassPath;
static bool				mConfigRead = false;
static JavaVMOption		mVMOptions [50];
static int				mNumVMOptions = 0;
static bool				mClassPathHasEntries = false;

static JavaVM *			mJVM = NULL;
static JNIEnv *			mEnv = NULL;
static jclass			mClassId = NULL;

static const size_t		JVM_KEY_Len = strlen (JVM_KEY);
static const size_t		CLASS_KEY_Len = strlen (CLASS_KEY);
static const size_t		CP_KEY_Len = strlen (CP_KEY);
static const size_t		LIB_KEY_Len = strlen (LIB_KEY);

static string getExeDirectory();

static char *					strnew (const char *s) {
	size_t		len = strlen (s);
	char *		ret = new char [len + 1];

#ifdef __GOT_SECURE_LIB__
    strcpy_s (ret, len + 1, s);
#else
    strcpy (ret, s);
#endif

	return (ret);
}

static void					destroyJVM () {
	if (mJVM == NULL) 
		return;

	if (mEnv != NULL && mEnv -> ExceptionOccurred ()) {
		mEnv -> ExceptionDescribe ();
	}

	mJVM -> DestroyJavaVM ();
	mJVM = NULL;
}

static void					err (int code, string diag, bool noFile = false) {
	char buff[50];
	time_t ltime;
	tm newtime;
	time( &ltime );
	#ifdef __GOT_SECURE_LIB__
		localtime_s( &newtime, &ltime );
	#else
		memcpy(localtime(&ltime), &newtime, sizeof(tm));
	#endif
	// Use strftime to build a customized time string: "Mar 23, 2009 7:10:07 AM"
	strftime( buff, sizeof(buff), "%b %d, %Y %I:%M:%S %p ", &newtime );

	cerr << buff << diag << endl;
	if(!noFile) {
		ofstream file;
		file.open((getExeDirectory() + "launcher.log").c_str(), ios_base::out | ios_base::app);
		file  << buff << diag << endl;
		file.close();
	}
	destroyJVM ();
	exit (code);
}

static string getExeDirectory() {
	char		cpath [2048];
	if (GetModuleFileNameA (NULL, cpath, sizeof (cpath)) == 0)
		err (1, "GetModuleFileNameA failed", true);

	string		path (cpath);

	size_t		idx = path.rfind ("\\");
	
	if (idx == string::npos)
		err (2, "Bad path: " + path, true);

	return path.substr (0, idx + 1);
}

static void					addEntryToClassPath (const string &s) {
	if (mClassPathHasEntries)
		mClassPath << ";";
	else
		mClassPathHasEntries = true;

    mClassPath << s;
}

static void					addLibToClassPath (const string &path) {
	string				pattern = path + "\\*.jar";

    WIN32_FIND_DATA		FindFileData;
    HANDLE              hFind = FindFirstFile (pattern.c_str (), &FindFileData);

    if (hFind != INVALID_HANDLE_VALUE) {  
		addEntryToClassPath (path + "\\" + FindFileData.cFileName);

        while (FindNextFile (hFind, &FindFileData)) 
            addEntryToClassPath (path + "\\" + FindFileData.cFileName);

        FindClose (hFind);
    }    
}

static void					readConfig () {
	if (mConfigRead)
		return;

	mClassPath << "-Djava.class.path=";

	string		configPath = getExeDirectory() + "config.txt";

	ifstream	ifs (configPath.c_str ());

	if (!ifs)
		err (3, "Cannot read: " + configPath);

	char				line [2048];
	char *				pstart;
	char *				pend;

	while (ifs.getline (line, sizeof (line))) {		
		pstart = line;
		pend = line + strlen (line);
		
		//Trim
		while (pstart < pend && isspace (*pstart))
			pstart++;

		while (pstart < pend && isspace (*pend))
			pend--;

		if (pstart == pend)
			continue;

		*pend = 0;

		if (strncmp (pstart, JVM_KEY, JVM_KEY_Len) == 0)
			mJVMDLLPath = pstart + JVM_KEY_Len;
		else if (strncmp (pstart, CLASS_KEY, CLASS_KEY_Len) == 0)
			mClassName = pstart + CLASS_KEY_Len;
		else if (strncmp (pstart, CP_KEY, CP_KEY_Len) == 0)
			addEntryToClassPath (pstart + CP_KEY_Len); 
		else if (strncmp (pstart, LIB_KEY, LIB_KEY_Len) == 0)
			addLibToClassPath (pstart + LIB_KEY_Len); 
		else 
			mVMOptions [mNumVMOptions++].optionString = strnew (pstart);
	}

	ifs.close();	

	mVMOptions [mNumVMOptions++].optionString = 
		strnew (mClassPath.str ().c_str ());

	mConfigRead = true;
}

static HMODULE				loadLibrarySilent(LPCTSTR pszPath) {
	UINT uMode = SetErrorMode(SEM_FAILCRITICALERRORS);
	HMODULE handle = LoadLibrary(pszPath);
	SetErrorMode(uMode);
	return handle;
}

inline void appendPATH(LPCSTR jvmPath) {
	// retrieve path to bin directory
	string path (jvmPath);
	size_t idx = path.rfind ('\\', path.length() - 9);
	path = path.substr(0, idx);
	path.insert(0, ";");

	LPCSTR pszPathName = "PATH";
	DWORD dwSize = GetEnvironmentVariable(pszPathName, NULL, 0);
	if(dwSize == 0) {
		err(12, "GetEnvironmentVariable err=" + GetLastError());
	}
	else {
		// and append it to %PATH%
		LPTSTR pszBuffer = new TCHAR[dwSize + path.length() + 1];
		dwSize = GetEnvironmentVariable(pszPathName, pszBuffer, dwSize);
		strcpy_s(pszBuffer + dwSize, path.length() + 1, path.c_str());

		BOOL bRes = SetEnvironmentVariable(pszPathName, pszBuffer);
		if(!bRes) {
			err(13, "SetEnvironmentVariable  err=" + GetLastError());
		}
		delete pszBuffer;
	}
}

static void					createJVM () {
	if (mJVM != NULL) 
		return;

	HINSTANCE       handle = loadLibrarySilent (mJVMDLLPath.c_str ());

	if (handle == 0) {
		DWORD dwErr = GetLastError();
		if(dwErr == ERROR_MOD_NOT_FOUND) {
			appendPATH (mJVMDLLPath.c_str ());
			handle = LoadLibrary(mJVMDLLPath.c_str ());
		}
		if(handle == 0) {
			DWORD dwErr = GetLastError();
			LPTSTR lpMsgBuf;
			DWORD dwErr2 = FormatMessage(
					FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
					NULL,
					dwErr,
					MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
					(LPTSTR) &lpMsgBuf,
					0, NULL );

			char buff[250];
			#ifdef __GOT_SECURE_LIB__
				if(dwErr2 == 0)
					sprintf_s(buff, sizeof(buff), " error=0x%x", dwErr);
				else
					sprintf_s(buff, sizeof(buff), " error=0x%x: %s", dwErr, lpMsgBuf);
			#else
				if(dwErr2 == 0)
					sprintf(buff, " error=0x%x", dwErr);
				else
					sprintf(buff, " error=0x%x: %s", dwErr, lpMsgBuf);
			#endif
			if(dwErr2 != 0)
				LocalFree(lpMsgBuf);

			ostringstream	os;
			os << "Cannot load " << mJVMDLLPath << buff;
			err (4, os.str ());
		}
	}

	t_CreateJavaVM	CreateJavaVM =
		(t_CreateJavaVM) GetProcAddress (handle, "JNI_CreateJavaVM");

	if (CreateJavaVM == NULL)
		err (5, "JNI_CreateJavaVM not found in " + mJVMDLLPath);

	JavaVMInitArgs			vm_args;

	vm_args.version = JNI_VERSION_1_4;
	vm_args.options = mVMOptions;
	vm_args.nOptions = mNumVMOptions;
	vm_args.ignoreUnrecognized = JNI_FALSE;
	
	int						res = 
		CreateJavaVM (&mJVM, (void **) &mEnv, &vm_args);

	if (res != 0)
		err (6, "JNI_CreateJavaVM failed");
}

static void					loadClass () {
	if (mClassId != NULL)
		return;

	mClassId = mEnv -> FindClass (mClassName.c_str ());

	if (mClassId == NULL)
		err (100, "Class not found: " + mClassName);
}

static void				callMethod (const char *name, int argc, char **argv) {
	jmethodID		mid = 
		mEnv -> GetStaticMethodID (mClassId, name, "([Ljava/lang/String;)V");

	if (mid == NULL) {
		ostringstream	os;
		os << "Method " << name << " not found in class " << mClassName;
		err (11, os.str ());
	}

	jclass			stringClass = mEnv -> FindClass ("java/lang/String");
	jobjectArray	args = mEnv -> NewObjectArray (argc, stringClass, NULL);

	for (int ii = 0; ii < argc; ii++)
		mEnv -> SetObjectArrayElement (args, ii, mEnv -> NewStringUTF (argv [ii]));

	mEnv -> CallStaticVoidMethod (mClassId, mid, args);
}

extern "C" int			main (int argc, char **argv) {
	readConfig ();
	createJVM ();
	loadClass ();
	callMethod ("main", argc - 1, argv + 1);
	return (0);
}
